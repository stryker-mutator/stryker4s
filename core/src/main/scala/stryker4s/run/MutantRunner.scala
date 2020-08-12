package stryker4s.run

import java.nio.file.Path

import scala.concurrent.duration._

import better.files.File
import grizzled.slf4j.Logging
import mutationtesting.{Metrics, MetricsResult}
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.extension.exception.InitialTestRunFailedException
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.mapper.MutantRunResultMapper
import stryker4s.report.{FinishedRunReport, Reporter}
import cats.effect.Resource
import cats.effect.IO
import cats.effect.Blocker
import fs2.{io, text, Pipe, Stream}
import cats.effect.ContextShift

abstract class MutantRunner(sourceCollector: SourceCollector, reporter: Reporter)(implicit
    config: Config,
    cs: ContextShift[IO]
) extends MutantRunResultMapper
    with Logging {
  type Context <: TestRunnerContext

  def apply(mutatedFiles: Iterable[MutatedFile]): IO[MetricsResult] =
    prepareEnv(mutatedFiles.toSeq).use { context =>
      initialTestRun(context)

      for {
        runResults <- IO(runMutants(mutatedFiles, context))
        report = toReport(runResults)
        metrics = Metrics.calculateMetrics(report)
        _ <- reporter.reportRunFinished(FinishedRunReport(report, metrics))
      } yield metrics
    }

  private def prepareEnv(mutatedFiles: Seq[MutatedFile]): Resource[IO, Context] = {
    val tmpDir: Path = {
      val targetFolder = config.baseDir / "target"
      targetFolder.createDirectoryIfNotExists()

      File.newTemporaryDirectory("stryker4s-", Some(targetFolder)).path
    }
    Resource
      .liftF(setupFiles(mutatedFiles, tmpDir))
      .flatMap { _ => initializeTestContext(tmpDir) }
  }

  private def setupFiles(mutatedFiles: Seq[MutatedFile], tmpDir: Path): IO[Unit] = {
    val mutatedPaths = mutatedFiles.map(_.fileOrigin)
    val unmutatedSourceFiles =
      Stream
        .evalSeq(IO(sourceCollector.filesToCopy.toSeq))
        .filter(mutatedPaths.contains)
        .map(_.path)

    val mutatedFilesStream = Stream.emits(mutatedFiles)

    Blocker[IO].use { blocker =>
      (unmutatedSourceFiles.through(writeOriginalFiles(blocker, tmpDir)) merge
        mutatedFilesStream.through(writeMutatedFiles(blocker, tmpDir))).compile.drain
    }
  }

  def writeOriginalFiles(blocker: Blocker, tmpDir: Path): Pipe[IO, Path, Unit] =
    files =>
      files.evalMap { file =>
        val newSubPath = file.inSubDir(tmpDir)
        io.file.copy[IO](blocker, file, newSubPath).void
      }

  def writeMutatedFiles(blocker: Blocker, tmpDir: Path): Pipe[IO, MutatedFile, Unit] =
    files =>
      files.flatMap { mutatedFile =>
        val targetPath = mutatedFile.fileOrigin.path.inSubDir(tmpDir)
        Stream(mutatedFile.tree.syntax)
          .covary[IO]
          .through(text.utf8Encode)
          .through(fs2.io.file.writeAll(targetPath, blocker))
      }

  private def runMutants(mutatedFiles: Iterable[MutatedFile], context: Context): Iterable[MutantRunResult] =
    for {
      mutatedFile <- mutatedFiles
      subPath = mutatedFile.fileOrigin.relativePath
      mutant <- mutatedFile.mutants
    } yield {
      val totalMutants = mutatedFiles.flatMap(_.mutants).size
      // TODO: Don't use unsafeRun
      reporter.reportMutationStart(mutant).unsafeRunTimed(5.seconds)
      val result = runMutant(mutant, context)(subPath)
      reporter.reportMutationComplete(result, totalMutants).unsafeRunTimed(5.seconds)
      result
    }

  def runMutant(mutant: Mutant, context: Context): Path => MutantRunResult

  def initialTestRun(context: Context): Unit = {
    info("Starting initial test run...")
    if (!runInitialTest(context)) {
      throw InitialTestRunFailedException(
        "Initial test run failed. Please make sure your tests pass before running Stryker4s."
      )
    }
    info("Initial test run succeeded! Testing mutants...")
  }

  def runInitialTest(context: Context): Boolean

  def initializeTestContext(tmpDir: File): Resource[IO, Context]

}
