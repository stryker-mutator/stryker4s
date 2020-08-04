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

abstract class MutantRunner(sourceCollector: SourceCollector, reporter: Reporter)(implicit
    config: Config
) extends MutantRunResultMapper
    with Logging {
  type Context <: TestRunnerContext

  def apply(mutatedFiles: Iterable[MutatedFile]): MetricsResult = {
    val context = prepareEnv(mutatedFiles)

    initialTestRun(context)

    val runResults = runMutants(mutatedFiles, context)

    val report = toReport(runResults)
    val metrics = Metrics.calculateMetrics(report)

    // Timeout of 15 seconds is a little longer to make sure http requests etc finish
    // TODO: Don't use unsafeRun
    reporter.reportRunFinished(FinishedRunReport(report, metrics)).unsafeRunTimed(15.seconds)
    metrics
  }

  private def prepareEnv(mutatedFiles: Iterable[MutatedFile]): Context = {
    val files = sourceCollector.filesToCopy
    val tmpDir: File = {
      val targetFolder = config.baseDir / "target"
      targetFolder.createDirectoryIfNotExists()

      File.newTemporaryDirectory("stryker4s-", Some(targetFolder))
    }

    debug("Using temp directory: " + tmpDir)

    files.foreach(copyFile(_, tmpDir))

    // Overwrite files to mutated files
    mutatedFiles.foreach(writeMutatedFile(_, tmpDir))
    initializeTestContext(tmpDir)
  }

  private def copyFile(file: File, tmpDir: File): Unit = {
    val filePath = tmpDir / file.relativePath.toString

    filePath.createIfNotExists(file.isDirectory, createParents = true)

    val _ = file.copyTo(filePath, overwrite = true)
  }

  private def writeMutatedFile(mutatedFile: MutatedFile, tmpDir: File): File = {
    val filePath = mutatedFile.fileOrigin.inSubDir(tmpDir)
    filePath.overwrite(mutatedFile.tree.syntax)
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

  def initializeTestContext(tmpDir: File): Context
}
