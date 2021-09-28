package stryker4s.mutants

import cats.effect.IO
import cats.syntax.functor._
import fs2.Stream
import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.extension.StreamExtensions._
import stryker4s.log.Logger
import stryker4s.model._
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.MutantFinder

class Mutator(
    mutantFinder: MutantFinder,
    transformer: StatementTransformer,
    matchBuilder: MatchBuilder
)(implicit
    config: Config,
    log: Logger
) {

  //Logic for dealing with compiler errors and removing non-compiling mutants from files
  private val rollbackHandler: RollbackHandler = new RollbackHandler(matchBuilder)

  def mutate(files: Stream[IO, Path], compileErrors: Seq[CompilerErrMsg] = Seq.empty): IO[Seq[MutatedFile]] = {
    files
      .parEvalMapUnordered(config.concurrency)(p => findMutants(p).tupleLeft(p))
      .map { case (file, mutationsInSource) =>
        if (compileErrors.isEmpty)
          mutateFile(file, mutationsInSource)
        else
          rollbackHandler.rollbackNonCompilingMutants(file, mutationsInSource, mutateFile, compileErrors)
      }
      .filterNot(mutatedFile => mutatedFile.mutants.isEmpty && mutatedFile.excludedMutants == 0)
      .compile
      .toVector
      .flatTap(logMutationResult)
  }

  private def mutateFile(
      file: Path,
      mutationsInSource: MutationsInSource
  ): MutatedFile = {
    val transformed = transformStatements(mutationsInSource)
    val builtTree = buildMatches(transformed)

    MutatedFile(
      fileOrigin = file,
      tree = builtTree,
      mutants = mutationsInSource.mutants,
      nonCompilingMutants = Seq.empty,
      excludedMutants = mutationsInSource.excluded
    )
  }

  /** Step 1: Find mutants in the found files
    */
  private def findMutants(file: Path): IO[MutationsInSource] = mutantFinder.mutantsInFile(file)

  /** Step 2: transform the statements of the found mutants (preparation of building pattern matches)
    */
  private def transformStatements(mutants: MutationsInSource): SourceTransformations =
    transformer.transformSource(mutants.source, mutants.mutants)

  /** Step 3: Build pattern matches from transformed trees
    */
  private def buildMatches(transformedMutantsInSource: SourceTransformations) =
    matchBuilder.buildNewSource(transformedMutantsInSource)

  private def logMutationResult(mutatedFiles: Iterable[MutatedFile]): IO[Unit] = {
    val includedMutants = mutatedFiles.flatMap(_.mutants).size
    val excludedMutants = mutatedFiles.map(_.excludedMutants).sum
    val totalMutants = includedMutants + excludedMutants

    def dryRunText(configProperty: String): String =
      s"""Stryker4s will perform a dry-run without actually mutating anything.
         |You can configure the `$configProperty` property in your configuration""".stripMargin

    IO(log.info(s"Found ${mutatedFiles.size} file(s) to be mutated.")) *>
      IO(
        log.info(
          s"$totalMutants Mutant(s) generated.${if (excludedMutants > 0) s" Of which $excludedMutants mutant(s) are excluded."}"
        )
      ) *> {
        if (includedMutants == 0 && excludedMutants > 0) {
          IO(log.warn(s"All found mutations are excluded. ${dryRunText("mutate` or `excluded-mutations")}"))
        } else if (totalMutants == 0) {
          IO(log.info("Files to be mutated are found, but no mutations were found in those files.")) *>
            IO(log.info("If this is not intended, please check your configuration and try again."))
        } else IO.unit
      }
  }
}
