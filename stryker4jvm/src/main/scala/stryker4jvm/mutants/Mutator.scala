package stryker4jvm.mutants

import cats.Functor
import cats.effect.IO
import cats.syntax.all.*
import fansi.Color
import fs2.io.file.Path
import fs2.{Chunk, Pipe, Stream}
import mutationtesting.{MutantResult, MutantStatus}
import stryker4jvm.extensions.Stryker4jvmCoreConversions.*

import scala.collection.JavaConverters.*
import stryker4jvm.config.Config
import stryker4jvm.core.logging.Logger
import stryker4jvm.core.model.CollectedMutants.IgnoredMutation
import stryker4jvm.core.model.{
  AST,
  CollectedMutants,
  CollectedMutantsWithId,
  LanguageMutator,
  MutantWithId,
  MutatedCode
}
import stryker4jvm.extensions.Stryker4jvmCoreConversions
import stryker4jvm.model.{MutantResultsPerFile, MutatedFile, SourceContext}

import java.util.concurrent.atomic.AtomicInteger
import java.util
import java.io.IOException

class Mutator(
    mutantRouter: Map[String, LanguageMutator[? <: AST]]
)(implicit
    config: Config,
    log: Logger
) {
  def go(files: Stream[IO, Path]): IO[(MutantResultsPerFile, Seq[MutatedFile])] = {
    files
      // Parse and mutate files
      .parEvalMap(config.concurrency) { path =>
        val mutator = mutantRouter(path.extName)
        try {
          val source = mutator.parse(path.toNioPath)
          val foundMutations = mutator.collect(source).asInstanceOf[CollectedMutants[AST]]

          IO((SourceContext(source, path), foundMutations))
        } catch {
          case e: IOException => IO.raiseError(e)
        }
      }
      // Give each mutation a unique id
      .through(updateWithId())
      // Split mutations into active and ignored mutations
      .flatMap { case (ctx, collectedWithId) =>
        splitIgnoredAndFound(ctx, collectedWithId.mutantResults, collectedWithId.mutations)
      }
      // Instrument files
      .parEvalMapUnordered(config.concurrency)(_.traverse { case (context, mutations) =>
        val mutator = mutantRouter(context.path.extName)
        val instrumented = mutator.instrument(context.source, mutations)
        val mutants = mutations.asScala.values.map(_.asScala.toVector).toVector.flatten
        val mutatedFile = MutatedFile(context.path, instrumented, mutants)
        IO(log.debug(s"Instrumenting mutations in ${mutations.size} places in ${context.path}")) *>
          IO(mutatedFile)
      })
      // Fold into 2 separate lists of ignored and found mutants (in files)
      .through(foldAndSplitEithers)
      .evalTap { case (ignored, files) => logMutationResult(ignored, files) }
      .compile
      .lastOrError
  }

  private def updateWithId()
      : Pipe[IO, (SourceContext, CollectedMutants[AST]), (SourceContext, CollectedMutantsWithId[AST])] = {

    def mapLeft(lefts: util.List[IgnoredMutation[AST]], i: AtomicInteger) = {
      lefts.asScala.map { ignored =>
        val reason = ignored.reason
        val mutation = ignored.mutatedCode
        MutantResult(
          i.getAndIncrement().toString,
          mutation.metaData.mutatorName,
          mutation.metaData.replacement,
          mutation.metaData.location.asMutationElement,
          MutantStatus.Ignored,
          statusReason = Some(reason.explanation)
        ).asCoreElement
      }.asJava
    }

    def mapRight(rights: util.Map[AST, util.List[MutatedCode[AST]]], i: AtomicInteger) = {
      //   // Functor to use a deep map instead of .map(_.map...)
      rights.asScala
        .mapValues(mutations => mutations.asScala.map(mut => new MutantWithId(i.getAndIncrement(), mut)).asJava)
        .toMap
        .asJava
    }

    _.scanChunks(new AtomicInteger()) { case (i, chunk) =>
      val out = Functor[Chunk]
        .compose[(SourceContext, *)]
        .map(chunk) { collected =>
          new CollectedMutantsWithId[AST](
            mapLeft(collected.ignoredMutations, i),
            mapRight(collected.mutations, i)
          )
        }

      (i, out)
    }
  }

  private def splitIgnoredAndFound(
      ctx: SourceContext,
      ignored: util.List[stryker4jvm.core.model.elements.MutantResult],
      found: util.Map[AST, util.List[MutantWithId[AST]]]
  ) = {
    val leftStream = Stream.emit((ctx.path, ignored.asScala.toVector.map(_.asMutationElement)).asLeft)
    val rightStream = if (found.size() != 0) Stream.emit((ctx, found).asRight) else Stream.empty
    leftStream ++ rightStream
  }

  private def logMutationResult(ignored: MutantResultsPerFile, mutatedFiles: Seq[MutatedFile]): IO[Unit] = {
    val totalFiles = (mutatedFiles.map(_.fileOrigin) ++ ignored.keys).distinct.size
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
