package stryker4s.mutants

import cats.effect.IO
import cats.syntax.functor._
import fs2.Stream
import fs2.io.file.Path
import stryker4s.CompileError
import stryker4s.config.Config
import stryker4s.extension.StreamExtensions._
import stryker4s.log.Logger
import stryker4s.model.{MutantId, MutatedFile, MutationsInSource, SourceTransformations}
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.MutantFinder

class Mutator(mutantFinder: MutantFinder, transformer: StatementTransformer, matchBuilder: MatchBuilder)(implicit
    config: Config,
    log: Logger
) {

  //Given compiler errors, return the mutants that caused it
  def errorsToIds(compileError: Seq[CompileError], files: Seq[MutatedFile]): Seq[MutantId] = {
    compileError.flatMap { err =>
      files
        //Find the file that the compiler error came from
        .find(_.fileOrigin.toString.endsWith(err.path))
        //Find the mutant case statement that cased the compiler error
        .flatMap(file => file.mutantLineNumbers.get(err.line))
    }
  }

  def mutate(files: Stream[IO, Path], nonCompilingMutants: Seq[MutantId] = Seq.empty): IO[Seq[MutatedFile]] = {
    files
      .parEvalMapUnordered(config.concurrency)(p => findMutants(p).tupleLeft(p))
      .map { case (file, mutationsInSource) =>
        val validMutants =
          mutationsInSource.mutants.filterNot(mut => nonCompilingMutants.exists(_.sameMutation(mut.id)))
        val transformed = transformStatements(mutationsInSource.copy(mutants = validMutants))
        val (builtTree, mutations) = buildMatches(transformed)

        MutatedFile(file, builtTree, mutationsInSource.mutants, mutations, mutationsInSource.excluded)
      }
      .filterNot(mutatedFile => mutatedFile.mutants.isEmpty && mutatedFile.excludedMutants == 0)
      .compile
      .toVector
      .flatTap(logMutationResult)
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
