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
import stryker4s.mutants.tree.{MutantCollector, MutantInstrumenter, MutantsWithId, Mutations}

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
  type FoundMutationsWithId = Found[MutantResult, MutantsWithId]

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
      .through(foldAndSplitEithers)
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
      found: Map[PlaceableTree, MutantsWithId]
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
          s"${Color.Cyan(totalMutants.toString())} mutant(s) generated.${(excludedMutants > 0)
              .guard[Option]
              .as(s" Of which ${Color.LightRed(excludedMutants.toString())} mutant(s) are excluded.")
              .orEmpty}"
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

  def foldAndSplitEithers[A, B, C]: Pipe[IO, Either[
    (A, B),
    C
  ], (Map[A, B], Vector[C])] =
    _.fold((Map.newBuilder[A, B], Vector.newBuilder[C])) {
      case ((l, r), Right(f)) =>
        (l, r += f)
      case ((l, r), Left(f)) =>
        (l += f, r)
    }.map { case (l, r) => (l.result(), r.result()) }

}
