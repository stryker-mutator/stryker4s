package stryker4s.run

import java.nio.file.Path

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
import cats.implicits._

abstract class MutantRunner(sourceCollector: SourceCollector, reporter: Reporter)(implicit
    config: Config,
    cs: ContextShift[IO]
) extends MutantRunResultMapper
    with Logging {
  type Context <: TestRunnerContext

  def apply(mutatedFiles: Iterable[MutatedFile]): IO[MetricsResult] =
    Blocker[IO].use { blocker =>
      io.file.createDirectories[IO](blocker, (config.baseDir / "target").path) *>
        io.file.tempDirectoryResource[IO](blocker, (config.baseDir / "target").path, "stryker4s-").use { tmpDir =>
          prepareEnv(blocker, tmpDir, mutatedFiles.toSeq).use { context =>
            for {
              _ <- IO(initialTestRun(context))
              runResults <- runMutants(mutatedFiles, context)
              report = toReport(runResults)
              metrics = Metrics.calculateMetrics(report)
              _ <- reporter.reportRunFinished(FinishedRunReport(report, metrics))
            } yield metrics
          }
        }
    }

  private def prepareEnv(blocker: Blocker, tmpDir: Path, mutatedFiles: Seq[MutatedFile]): Resource[IO, Context] = {
    Resource.liftF(setupFiles(blocker, tmpDir, mutatedFiles)) *>
      initializeTestContext(tmpDir)
  }

  private def setupFiles(blocker: Blocker, tmpDir: Path, mutatedFiles: Seq[MutatedFile]): IO[Unit] = {
    val mutatedPaths = mutatedFiles.map(_.fileOrigin)
    val unmutatedFilesStream =
      Stream
        .evalSeq(IO(sourceCollector.filesToCopy.toSeq))
        .filter(mutatedPaths.contains)
        .map(_.path)
        .through(writeOriginalFile(blocker, tmpDir))

    val mutatedFilesStream = Stream
      .emits(mutatedFiles)
      .through(writeMutatedFile(blocker, tmpDir))

    (unmutatedFilesStream merge
      mutatedFilesStream).compile.drain
  }

  def writeOriginalFile(blocker: Blocker, tmpDir: Path): Pipe[IO, Path, Unit] =
    files =>
      files.evalMap { file =>
        val newSubPath = file.inSubDir(tmpDir)
        io.file.copy[IO](blocker, file, newSubPath).void
      }

  def writeMutatedFile(blocker: Blocker, tmpDir: Path): Pipe[IO, MutatedFile, Unit] =
    files =>
      files.flatMap { mutatedFile =>
        val targetPath = mutatedFile.fileOrigin.path.inSubDir(tmpDir)
        Stream(mutatedFile.tree.syntax)
          .covary[IO]
          .through(text.utf8Encode)
          .through(fs2.io.file.writeAll(targetPath, blocker))
      }

  private def runMutants(mutatedFiles: Iterable[MutatedFile], context: Context): IO[List[MutantRunResult]] =
    mutatedFiles.toList.flatTraverse { mutatedFile =>
      val subPath = mutatedFile.fileOrigin.relativePath
      mutatedFile.mutants.toList.traverse { mutant =>
        val totalMutants = mutatedFiles.flatMap(_.mutants).size

        reporter.reportMutationStart(mutant) *>
          runMutant(mutant, context, subPath).flatTap(
            reporter.reportMutationComplete(_, totalMutants)
          )
      }
    }

  def runMutant(mutant: Mutant, context: Context, subPath: Path): IO[MutantRunResult]

  def initialTestRun(context: Context): IO[Unit] = {
    IO(info("Starting initial test run...")) *>
      runInitialTest(context).flatMap { result =>
        if (!result) {
          IO.raiseError(
            InitialTestRunFailedException(
              "Initial test run failed. Please make sure your tests pass before running Stryker4s."
            )
          )
        }
        IO(info("Initial test run succeeded! Testing mutants..."))
      }
  }

  def runInitialTest(context: Context): IO[Boolean]

  def initializeTestContext(tmpDir: File): Resource[IO, Context]

}
