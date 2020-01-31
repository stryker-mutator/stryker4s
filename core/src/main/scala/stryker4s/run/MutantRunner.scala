package stryker4s.run

import java.nio.file.Path

import better.files.File
import grizzled.slf4j.Logging
import mutationtesting.{Metrics, MetricsResult}
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.report.mapper.MutantRunResultMapper
import stryker4s.report.FinishedRunReport

abstract class MutantRunner(sourceCollector: SourceCollector, reporter: Reporter)(implicit config: Config)
    extends InitialTestRun
    with MutantRunResultMapper
    with Logging {
  val tmpDir: File = {
    val targetFolder = config.baseDir / "target"
    targetFolder.createDirectoryIfNotExists()

    File.newTemporaryDirectory("stryker4s-", Some(targetFolder))
  }

  def apply(mutatedFiles: Iterable[MutatedFile]): MetricsResult = {
    prepareEnv(mutatedFiles)

    initialTestRun(tmpDir)

    val runResults = runMutants(mutatedFiles)

    val report = toReport(runResults)
    val metrics = Metrics.calculateMetrics(report)

    reporter.reportRunFinished(FinishedRunReport(report, metrics))
    metrics
  }

  private def prepareEnv(mutatedFiles: Iterable[MutatedFile]): Unit = {
    val files = sourceCollector.filesToCopy

    debug("Using temp directory: " + tmpDir)

    files.foreach(copyFile)

    // Overwrite files to mutated files
    mutatedFiles.foreach(writeMutatedFile)
  }

  private def copyFile(file: File): Unit = {
    val filePath = tmpDir / file.relativePath.toString

    filePath.createIfNotExists(file.isDirectory, createParents = true)

    val _ = file.copyTo(filePath, overwrite = true)
  }

  private def writeMutatedFile(mutatedFile: MutatedFile): File = {
    val filePath = mutatedFile.fileOrigin.inSubDir(tmpDir)
    filePath.overwrite(mutatedFile.tree.syntax)
  }

  private def runMutants(mutatedFiles: Iterable[MutatedFile]): Iterable[MutantRunResult] =
    for {
      mutatedFile <- mutatedFiles
      subPath = mutatedFile.fileOrigin.relativePath
      mutant <- mutatedFile.mutants
    } yield {
      val totalMutants = mutatedFiles.flatMap(_.mutants).size
      reporter.reportMutationStart(mutant)
      val result = runMutant(mutant, tmpDir)(subPath)
      reporter.reportMutationComplete(result, totalMutants)
      result
    }

  def runMutant(mutant: Mutant, workingDir: File): Path => MutantRunResult
}
