package stryker4s.run

import cats.effect.IO
import stryker4s.model.MutatedFile
import mutationtesting.Metrics
import stryker4s.report.{FinishedRunReport, Reporter}
import mutationtesting.MetricsResult
import better.files.File
import stryker4s.config.Config
import cats.effect.Resource
import stryker4s.model.TestRunnerContext
import fs2.Stream
import cats.effect.Blocker
import stryker4s.mutants.findmutants.SourceCollector
import java.nio.file.Path
import cats.effect.ContextShift
import fs2.text
import stryker4s.extension.FileExtensions._
import fs2.Pipe
import grizzled.slf4j.Logging
import stryker4s.extension.exception.InitialTestRunFailedException
import stryker4s.report.mapper.MutantRunResultMapper
import stryker4s.model.MutantRunResult
import cats.implicits._
import stryker4s.model.Mutant
abstract class MutationRunOrchestrator(sourceCollector: SourceCollector, reporter: Reporter)(implicit
    config: Config,
    cs: ContextShift[IO]
) extends MutantRunResultMapper
    with Logging {
  type Context <: TestRunnerContext

  def startMutationRun(mutatedFiles: Seq[MutatedFile]): IO[MetricsResult] =
    prepareEnv(mutatedFiles).use { context =>
      initialTestRun(context)

      for {
        runResults <- runMutants(mutatedFiles, context)
        report = toReport(runResults)
        metrics = Metrics.calculateMetrics(report)
        _ <- reporter.reportRunFinished(FinishedRunReport(report, metrics))
      } yield metrics
    }

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

  def prepareEnv(mutatedFiles: Seq[MutatedFile]): Resource[IO, Context] = {

    val tmpDir: Path = {
      val targetFolder = config.baseDir / "target"
      targetFolder.createDirectoryIfNotExists()

      File.newTemporaryDirectory("stryker4s-", Some(targetFolder)).path
    }
    Resource
      .liftF(setupFiles(mutatedFiles, tmpDir))
      .flatMap { _ => initializeTestContext(tmpDir) }
  }

  private def runMutants(mutatedFiles: Iterable[MutatedFile], context: Context): IO[List[MutantRunResult]] =
    mutatedFiles.toList.flatTraverse { mutatedFile =>
      val subPath = mutatedFile.fileOrigin.relativePath
      mutatedFile.mutants.toList.traverse { mutant =>
        val totalMutants = mutatedFiles.flatMap(_.mutants).size

        reporter.reportMutationStart(mutant) *>
          runMutant(mutant, context).map(_(subPath)).flatTap(reporter.reportMutationComplete(_, totalMutants))
      }
    }

  def runMutant(mutant: Mutant, context: Context): IO[Path => MutantRunResult]

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
        val newSubPath = tmpDir.resolve(file)
        fs2.io.file.copy[IO](blocker, file, newSubPath).void
      }

  def writeMutatedFiles(blocker: Blocker, tmpDir: Path): Pipe[IO, MutatedFile, Unit] =
    files =>
      files.flatMap { mutatedFile =>
        val targetPath = mutatedFile.fileOrigin.inSubDir(File(tmpDir)).path
        Stream(mutatedFile.tree.syntax)
          .covary[IO]
          .through(text.base64Decode)
          .through(fs2.io.file.writeAll(targetPath, blocker))
      }

  def initializeTestContext(tmpDir: File): Resource[IO, Context]

}
