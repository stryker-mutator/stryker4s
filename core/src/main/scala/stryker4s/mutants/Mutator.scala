package stryker4s.mutants

import cats.Functor
import cats.data.NonEmptyVector
import cats.effect.IO
import cats.kernel.Order
import cats.syntax.all.*
import fansi.Color
import fs2.io.file.Path
import fs2.{Chunk, Pipe, Stream}
import mutationtesting.{MutantResult, MutantStatus}
import stryker4s.config.Config
import stryker4s.log.Logger
import stryker4s.model.*
import stryker4s.mutants.findmutants.MutantFinder
import stryker4s.mutants.tree.{MutantCollector, MutantInstrumenter, Mutations, MutationsWithId}

import java.util.concurrent.atomic.AtomicInteger

class Mutator(
    mutantFinder: MutantFinder,
    collector: MutantCollector,
    instrumenter: MutantInstrumenter
)(implicit
    config: Config,
    log: Logger
) {

  type Found[A, B] = (SourceContext, (Vector[A], Map[PlaceableTree, B]))
  type FoundMutations = Found[(MutatedCode, IgnoredMutationReason), Mutations]
  type FoundMutationsWithId = Found[MutantResult, MutationsWithId]

  def go(files: Stream[IO, Path]): IO[(MutantResultsPerFile, Seq[MutatedFile])] = {
    files
      // Parse and mutate files
      .parEvalMap(config.concurrency)(path =>
        mutantFinder.parseFile(path).map { source =>
          val foundMutations = collector(source)

          (SourceContext(source, path), foundMutations)
        }
      )
      // Give each mutation a unique id
      .through(updateWithId)
      // Split mutations into active and ignored mutations
      .flatMap { case (ctx, (ignored, found)) => splitIgnoredAndFound(ctx, ignored, found) }
      // Instrument files
      .parEvalMapUnordered(config.concurrency)(_.traverse { case (context, mutations) =>
        IO(log.debug(s"Instrumenting mutations in ${mutations.size} places in ${context.path}")) *>
          IO(instrumenter.instrumentFile(context, mutations))
      })
      // Fold into 2 separate lists of ignored and found mutants (in files)
      .fold((Map.newBuilder[Path, Vector[MutantResult]], Vector.newBuilder[MutatedFile])) {
        case ((l, r), Right(f)) =>
          (l, r += f)
        case ((l, r), Left(f)) =>
          (l += f, r)
      }
      .map { case (l, r) => (l.result(), r.result()) }
      .evalTap { case (ignored, files) => logMutationResult(ignored, files) }
      .compile
      .lastOrError
  }

  private def updateWithId: Pipe[IO, FoundMutations, FoundMutationsWithId] = {

    def mapLeft(lefts: Vector[(MutatedCode, IgnoredMutationReason)], i: AtomicInteger) =
      lefts.map { case (mutated, reason) =>
        MutantResult(
          i.getAndIncrement().toString(),
          mutated.metadata.mutatorName,
          mutated.metadata.replacement,
          mutated.metadata.location,
          MutantStatus.Ignored,
          statusReason = Some(reason.explanation)
        )
      }

    def mapRight(rights: Map[PlaceableTree, Mutations], i: AtomicInteger) =
      //   // Functor to use a deep map instead of .map(_.map...)
      Functor[Map[PlaceableTree, *]]
        .compose[NonEmptyVector]
        .map(rights)(m => MutantWithId(MutantId(i.getAndIncrement()), m))

    _.scanChunks(new AtomicInteger()) { case (i, chunk) =>
      val out = Functor[Chunk]
        .compose[(SourceContext, *)]
        .map(chunk) { case (l, r) =>
          mapLeft(l, i) -> mapRight(r, i)
        }

      (i, out)
    }
  }

  private def splitIgnoredAndFound(
      ctx: SourceContext,
      ignored: Vector[MutantResult],
      found: Map[PlaceableTree, MutationsWithId]
  ) = {
    val leftStream = Stream.emit((ctx.path, ignored).asLeft)

    implicit val ordering = Order.by[PlaceableTree, String](p => p.tree.structure)
    val f = found.toVector.toNev.map(_.toNem).tupleLeft(ctx)
    val rightStream = Stream.fromOption(f).map(_.asRight)

    leftStream ++ rightStream
  }

  private def logMutationResult(ignored: MutantResultsPerFile, mutatedFiles: Seq[MutatedFile]): IO[Unit] = {
    val totalFiles = (mutatedFiles.map(_.fileOrigin) ++ ignored.map(_._1)).distinct.size
    val includedMutants = mutatedFiles.map(_.mutants.size).sum
    val excludedMutants = ignored.map(_._2.size).sum
    val totalMutants = includedMutants + excludedMutants

    def dryRunText(configProperties: String*): String =
      s"""Stryker4s will perform a dry-run without actually mutating anything.
         |You can configure the `${configProperties.mkString("` or `")}` property in your configuration""".stripMargin

    IO(log.info(s"Found ${Color.Cyan(totalFiles.toString())} file(s) to be mutated.")) *>
      IO(
        log.info(
          s"${Color.Cyan(totalMutants.toString())} mutant(s) generated.${if (excludedMutants > 0)
              s" Of which ${Color.LightRed(excludedMutants.toString())} mutant(s) are excluded."
            else ""}"
        )
      ) *> {
        if (includedMutants == 0 && excludedMutants > 0)
          IO(log.warn(s"All found mutations are excluded. ${dryRunText("mutate", "excluded-mutations")}"))
        else
          IO.whenA(totalMutants == 0) {
            IO(log.info("Files to be mutated are found, but no mutations were found in those files.")) *>
              IO(log.info("If this is not intended, please check your configuration and try again."))
          }
      }
  }

  // def mutate(files: Stream[IO, Path], compileErrors: Seq[CompilerErrMsg] = Seq.empty): IO[Seq[MutatedFile]] = {
  //   if (compileErrors.nonEmpty) {
  //     log.debug("Trying to remove mutants that gave these errors:\n\t" + compileErrors.mkString("\n\t"))
  //   }
  //   files
  //     .parEvalMapUnordered(config.concurrency)(p => findMutants(p).tupleLeft(p))
  //     .map { case (file, mutationsInSource) =>
  //       mutateFile(file, mutationsInSource, compileErrors)
  //     }
  //     .filterNot(mutatedFile => mutatedFile.mutants.isEmpty && mutatedFile.excludedMutants == 0)
  //     .compile
  //     .toVector
  //     .flatTap(logMutationResult)
  // }

  // private def mutateFile(
  //     file: Path,
  //     mutationsInSource: MutationsInSource,
  //     compileErrors: Seq[CompilerErrMsg]
  // ): MutatedFile = {
  //   val transformed = transformStatements(mutationsInSource)
  //   val builtTree = buildMatches(transformed)

  //   val mutatedFile = MutatedFile(
  //     fileOrigin = file,
  //     tree = builtTree,
  //     mutants = mutationsInSource.mutants,
  //     nonCompilingMutants = Seq.empty,
  //     excludedMutants = mutationsInSource.excluded
  //   )

  //   if (compileErrors.isEmpty)
  //     mutatedFile
  //   else {
  //     // If there are any compiler errors (i.e. we're currently retrying the mutation with the bad ones rolled back)
  //     // Then we take the original tree built that didn't compile
  //     // And then we search inside it to translate the compile errors to mutants
  //     // Finally we rebuild it from scratch without those mutants
  //     // This is not very performant, but you only pay the cost if there actually is a compiler error
  //     val errorsInThisFile = compileErrors.filter(err => file.endsWith(err.path))
  //     if (errorsInThisFile.isEmpty) {
  //       log.debug(s"No compiler errors in $file")
  //       mutatedFile
  //     } else {
  //       log.debug(s"Found ${errorsInThisFile.mkString(" ")} in $file")

  //       val nonCompilingIds = errorsToIds(
  //         errorsInThisFile,
  //         mutatedFile.mutatedSource,
  //         transformed.transformedStatements.flatMap(_.mutantStatements)
  //       )
  //       log.debug(s"Removed mutant id[s] ${nonCompilingIds.mkString(";")} in $file")

  //       val (nonCompilingMutants, compilingMutants) =
  //         mutationsInSource.mutants.partition(mut => nonCompilingIds.contains(mut.id))

  //       val mutationsInSourceWithoutErrors = mutationsInSource.copy(mutants = compilingMutants)
  //       val transformedWithoutErrors = transformStatements(mutationsInSourceWithoutErrors)
  //       val builtTreeWithoutErrors = buildMatches(transformedWithoutErrors)

  //       MutatedFile(
  //         fileOrigin = file,
  //         tree = builtTreeWithoutErrors,
  //         mutants = compilingMutants,
  //         nonCompilingMutants = nonCompilingMutants,
  //         excludedMutants = mutationsInSource.excluded
  //       )
  //     }
  //   }
  // }

  /** Step 1: Find mutants in the found files
    */
  // private def findMutants(file: Path): IO[MutationsInSource] = mutantFinder.mutantsInFile(file)

  // /** Step 2: transform the statements of the found mutants (preparation of building pattern matches)
  //   */
  // private def transformStatements(mutants: MutationsInSource): SourceTransformations =
  //   transformer.transformSource(mutants.source, mutants.mutants)

  // /** Step 3: Build pattern matches from transformed trees
  //   */
  // private def buildMatches(transformedMutantsInSource: SourceTransformations) =
  //   matchBuilder.buildNewSource(transformedMutantsInSource)

  // // Given compiler errors, return the mutants that caused it by searching for the matching case statement at that line
  // private def errorsToIds(
  //     compileErrors: Seq[CompilerErrMsg],
  //     mutatedFile: String,
  //     mutants: Seq[Mutant]
  // ): Seq[MutantId] = {
  //   val statementToMutIdMap = mutants.map { mutant =>
  //     matchBuilder.mutantToCase(mutant).structure -> mutant.id
  //   }.toMap

  //   val lineToMutantId: Map[Int, MutantId] = mutatedFile
  //     // Parsing the mutated tree again as a string is the only way to get the position info of the mutated statements
  //     .parse[Source]
  //     .getOrElse(throw new RuntimeException(s"Failed to parse $mutatedFile to remove non-compiling mutants"))
  //     .collect {
  //       case node: Case if statementToMutIdMap.contains(node.structure) =>
  //         val mutId = statementToMutIdMap(node.structure)
  //         // +1 because scalameta uses zero-indexed line numbers
  //         (node.pos.startLine to node.pos.endLine).map(i => i + 1 -> mutId)
  //     }
  //     .flatten
  //     .toMap

  //   compileErrors.flatMap { err =>
  //     lineToMutantId.get(err.line)
  //   }
  // }
}
