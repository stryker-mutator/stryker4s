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

import scala.meta._

class Mutator(mutantFinder: MutantFinder, transformer: StatementTransformer, matchBuilder: MatchBuilder)(implicit
    config: Config,
    log: Logger
) {

  def mutate(files: Stream[IO, Path], compileErrors: Seq[CompileError] = Seq.empty): IO[Seq[MutatedFile]] = {
    files
      .parEvalMapUnordered(config.concurrency)(p => findMutants(p).tupleLeft(p))
      .map { case (file, mutationsInSource) =>
        val errorsInThisFile = compileErrors.filter(err => file.toString.endsWith(err.path))
        mutateFile(file, mutationsInSource, errorsInThisFile)
      }
      .filterNot(mutatedFile => mutatedFile.mutants.isEmpty && mutatedFile.excludedMutants == 0)
      .compile
      .toVector
      .flatTap(logMutationResult)
  }

  private def mutateFile(
      file: Path,
      mutationsInSource: MutationsInSource,
      compileErrors: Seq[CompileError]
  ): MutatedFile = {
    val transformed = transformStatements(mutationsInSource)
    val (builtTree, mutations) = buildMatches(transformed)

    //If there are any compiler errors (i.e. we're currently retrying the mutation)
    //Then we take the original tree built that didn't compile
    //And then we search inside it to translate the compile errors to mutants
    //Finally we rebuild it from scratch without those mutants
    //This is not very performant, but you only pay the cost if there actually is a compiler error
    val mutatedFile = MutatedFile(file, builtTree, mutationsInSource.mutants, Seq.empty, mutationsInSource.excluded)
    if (compileErrors.isEmpty) {
      mutatedFile
    } else {
      val nonCompilingIds = errorsToIds(compileErrors, mutatedFile.mutatedSource, mutations)
      val (nonCompilingMutants, compilingMutants) =
        mutationsInSource.mutants.partition(mut => nonCompilingIds.contains(mut.id))

      val transformed = transformStatements(mutationsInSource.copy(mutants = compilingMutants))
      val (builtTree, _) = buildMatches(transformed)
      MutatedFile(file, builtTree, compilingMutants, nonCompilingMutants, mutationsInSource.excluded)
    }
  }

  //Given compiler errors, return the mutants that caused it
  private def errorsToIds(
      compileErrors: Seq[CompileError],
      mutatedFile: String,
      mutants: Seq[(MutantId, Case)]
  ): Seq[MutantId] = {
    val statementToMutIdMap = mutants.map { case (mutantId, mutationStatement) =>
      mutationStatement.structure -> mutantId
    }.toMap

    val lineToMutantId: Map[Int, MutantId] = mutatedFile
      //Parsing the mutated tree again as a string is the only way to get the position info of the mutated statements
      .parse[Source]
      .getOrElse(throw new RuntimeException(s"Failed to parse $mutatedFile to remove non-compiling mutants"))
      .collect {
        case node if statementToMutIdMap.contains(node.structure) =>
          val mutId = statementToMutIdMap(node.structure)
          //+1 because scalameta uses zero-indexed line numbers
          (node.pos.startLine to node.pos.endLine).map(i => i + 1 -> mutId)
      }
      .flatten
      .toMap

    compileErrors.flatMap { err =>
      lineToMutantId.get(err.line)
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
