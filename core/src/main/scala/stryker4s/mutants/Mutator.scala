package stryker4s.mutants

import cats.effect.IO
import cats.syntax.functor.*
import fansi.Color
import fs2.Stream
import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.extension.StreamExtensions.*
import stryker4s.log.Logger
import stryker4s.model.*
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.MutantFinder

import scala.meta.*

class Mutator(
    mutantFinder: MutantFinder,
    transformer: StatementTransformer,
    matchBuilder: MatchBuilder
)(implicit
    config: Config,
    log: Logger
) {

  def mutate(files: Stream[IO, Path], compileErrors: Seq[CompilerErrMsg] = Seq.empty): IO[Seq[MutatedFile]] = {
    if (compileErrors.nonEmpty) {
      log.debug("Trying to remove mutants that gave these errors:\n\t" + compileErrors.mkString("\n\t"))
    }
    files
      .parEvalMapUnordered(config.concurrency)(p => findMutants(p).tupleLeft(p))
      .map { case (file, mutationsInSource) =>
        mutateFile(file, mutationsInSource, compileErrors)
      }
      .filterNot(mutatedFile => mutatedFile.mutants.isEmpty && mutatedFile.excludedMutants == 0)
      .compile
      .toVector
      .flatTap(logMutationResult)
  }

  private def mutateFile(
      file: Path,
      mutationsInSource: MutationsInSource,
      compileErrors: Seq[CompilerErrMsg]
  ): MutatedFile = {
    val transformed = transformStatements(mutationsInSource)
    val builtTree = buildMatches(transformed)

    val mutatedFile = MutatedFile(
      fileOrigin = file,
      tree = builtTree,
      mutants = mutationsInSource.mutants,
      nonCompilingMutants = Seq.empty,
      excludedMutants = mutationsInSource.excluded
    )

    if (compileErrors.isEmpty)
      mutatedFile
    else {
      // If there are any compiler errors (i.e. we're currently retrying the mutation with the bad ones rolled back)
      // Then we take the original tree built that didn't compile
      // And then we search inside it to translate the compile errors to mutants
      // Finally we rebuild it from scratch without those mutants
      // This is not very performant, but you only pay the cost if there actually is a compiler error
      val errorsInThisFile = compileErrors.filter(err => file.endsWith(err.path))
      if (errorsInThisFile.isEmpty) {
        log.debug(s"No compiler errors in $file")
        mutatedFile
      } else {
        log.debug(s"Found ${errorsInThisFile.mkString(" ")} in $file")

        val nonCompilingIds = errorsToIds(
          errorsInThisFile,
          mutatedFile.mutatedSource,
          transformed.transformedStatements.flatMap(_.mutantStatements)
        )
        log.debug(s"Removed mutant id[s] ${nonCompilingIds.mkString(";")} in $file")

        val (nonCompilingMutants, compilingMutants) =
          mutationsInSource.mutants.partition(mut => nonCompilingIds.contains(mut.id))

        val mutationsInSourceWithoutErrors = mutationsInSource.copy(mutants = compilingMutants)
        val transformedWithoutErrors = transformStatements(mutationsInSourceWithoutErrors)
        val builtTreeWithoutErrors = buildMatches(transformedWithoutErrors)

        MutatedFile(
          fileOrigin = file,
          tree = builtTreeWithoutErrors,
          mutants = compilingMutants,
          nonCompilingMutants = nonCompilingMutants,
          excludedMutants = mutationsInSource.excluded
        )
      }
    }
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

    IO(log.info(s"Found ${Color.Cyan(mutatedFiles.size.toString())} file(s) to be mutated.")) *>
      IO(
        log.info(
          s"${Color.Cyan(totalMutants.toString())} mutant(s) generated.${if (excludedMutants > 0)
              s" Of which ${Color.LightRed(excludedMutants.toString())} mutant(s) are excluded."
            else ""}"
        )
      ) *> {
        if (includedMutants == 0 && excludedMutants > 0) {
          IO(log.warn(s"All found mutations are excluded. ${dryRunText("mutate` or `excluded-mutations")}"))
        } else
          IO.whenA(totalMutants == 0) {
            IO(log.info("Files to be mutated are found, but no mutations were found in those files.")) *>
              IO(log.info("If this is not intended, please check your configuration and try again."))
          }
      }
  }

  // Given compiler errors, return the mutants that caused it by searching for the matching case statement at that line
  private def errorsToIds(
      compileErrors: Seq[CompilerErrMsg],
      mutatedFile: String,
      mutants: Seq[Mutant]
  ): Seq[MutantId] = {
    val statementToMutIdMap = mutants.map { mutant =>
      matchBuilder.mutantToCase(mutant).structure -> mutant.id
    }.toMap

    val lineToMutantId: Map[Int, MutantId] = mutatedFile
      // Parsing the mutated tree again as a string is the only way to get the position info of the mutated statements
      .parse[Source]
      .getOrElse(throw new RuntimeException(s"Failed to parse $mutatedFile to remove non-compiling mutants"))
      .collect {
        case node: Case if statementToMutIdMap.contains(node.structure) =>
          val mutId = statementToMutIdMap(node.structure)
          // +1 because scalameta uses zero-indexed line numbers
          (node.pos.startLine to node.pos.endLine).map(i => i + 1 -> mutId)
      }
      .flatten
      .toMap

    compileErrors.flatMap { err =>
      lineToMutantId.get(err.line)
    }
  }
}
