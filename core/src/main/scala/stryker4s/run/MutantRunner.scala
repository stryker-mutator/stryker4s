package stryker4s.run

import java.nio.file.Path

import better.files.File
import cats.effect.{Blocker, ContextShift, IO, Resource}
import cats.implicits._
import fs2.{io, text, Pipe, Stream}
import grizzled.slf4j.Logging
import mutationtesting.{Metrics, MetricsResult}
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.extension.exception.InitialTestRunFailedException
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.mapper.MutantRunResultMapper
import stryker4s.report.{FinishedRunReport, Reporter}

abstract class MutantRunner(sourceCollector: SourceCollector, reporter: Reporter)(implicit
    config: Config,
    cs: ContextShift[IO]
) extends MutantRunResultMapper
    with Logging {
  type Context <: TestRunnerContext

  def apply(mutatedFiles: Iterable[MutatedFile]): IO[MetricsResult] =
    prepareEnv(mutatedFiles.toSeq).use { context =>
      for {
        _ <- initialTestRun(context)
        runResults <- runMutants(mutatedFiles, context)
        report = toReport(runResults)
        metrics = Metrics.calculateMetrics(report)
        _ <- reporter.reportRunFinished(FinishedRunReport(report, metrics))
      } yield metrics
    }

  def prepareEnv(mutatedFiles: Seq[MutatedFile]): Resource[IO, Context] =
    for {
      blocker <- Blocker[IO]
      targetDir = (config.baseDir / "target").path
      _ <- Resource.liftF(io.file.createDirectories[IO](blocker, targetDir))
      tmpDir <- io.file.tempDirectoryResource[IO](blocker, targetDir, "stryker4s-")
      _ <- Resource.liftF(setupFiles(blocker, tmpDir, mutatedFiles.toSeq))
      context <- initializeTestContext(tmpDir)
    } yield context

  private def setupFiles(blocker: Blocker, tmpDir: Path, mutatedFiles: Seq[MutatedFile]): IO[Unit] =
    IO(info("Setting up mutated environment...")) *>
      IO(debug("Using temp directory: " + tmpDir)) *> {
      val mutatedPaths = mutatedFiles.map(_.fileOrigin)
      val unmutatedFilesStream =
        Stream
          .evalSeq(IO(sourceCollector.filesToCopy.toSeq))
          .filterNot(mutatedPaths.contains)
          .map(_.path)
          .through(writeOriginalFile(blocker, tmpDir))

      val mutatedFilesStream = Stream
        .emits(mutatedFiles)
        .through(writeMutatedFile(blocker, tmpDir))

      (unmutatedFilesStream merge
        mutatedFilesStream).compile.drain
    }

  def writeOriginalFile(blocker: Blocker, tmpDir: Path): Pipe[IO, Path, Unit] =
    in =>
      in.evalMapChunk { file =>
        val newSubPath = file.inSubDir(tmpDir)

        IO(debug(s"Copying $file to $newSubPath")) *>
          io.file.createDirectories[IO](blocker, newSubPath.getParent()) *>
          io.file.copy[IO](blocker, file, newSubPath).void
      }

  def writeMutatedFile(blocker: Blocker, tmpDir: Path): Pipe[IO, MutatedFile, Unit] =
    in =>
      in.evalMapChunk { mutatedFile =>
        val targetPath = mutatedFile.fileOrigin.path.inSubDir(tmpDir)
        IO(debug(s"Writing ${mutatedFile.fileOrigin} file to $targetPath")) *>
          io.file.createDirectories[IO](blocker, targetPath.getParent()) *>
          IO.pure((mutatedFile, targetPath))
      }.flatMap {
        case (mutatedFile, targetPath) =>
          Stream(mutatedFile.tree.syntax)
            .covary[IO]
            .through(text.utf8Encode)
            .through(io.file.writeAll(targetPath, blocker))
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
        if (!result)
          IO.raiseError(
            InitialTestRunFailedException(
              "Initial test run failed. Please make sure your tests pass before running Stryker4s."
            )
          )
        else
          IO(info("Initial test run succeeded! Testing mutants..."))
      }
  }

  def runInitialTest(context: Context): IO[Boolean]

  def initializeTestContext(tmpDir: File): Resource[IO, Context]

}
