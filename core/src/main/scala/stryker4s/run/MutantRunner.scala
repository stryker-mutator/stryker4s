package stryker4s.run

import java.nio.file.Path

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.extension.score.MutationScoreCalculator
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.process.ProcessRunner

import scala.concurrent.duration.{Duration, MILLISECONDS}

abstract class MutantRunner(process: ProcessRunner, sourceCollector: SourceCollector)(implicit config: Config)
    extends InitialTestRun
    with MutationScoreCalculator
    with Logging {

  val tmpDir: File = {
    val targetFolder = config.baseDir / "target"
    targetFolder.createDirectoryIfNotExists()

    File.newTemporaryDirectory("stryker4s-", Some(targetFolder))
  }

  def apply(mutatedFiles: Iterable[MutatedFile]): MutantRunResults = {
    prepareEnv(mutatedFiles)

    initialTestRun(tmpDir)

    val startTime = System.currentTimeMillis()

    val runResults = runMutants(mutatedFiles)

    val duration = Duration(System.currentTimeMillis() - startTime, MILLISECONDS)
    val detected = runResults collect { case d: Detected => d }

    MutantRunResults(runResults, calculateMutationScore(runResults.size, detected.size), duration)
  }

  private def prepareEnv(mutatedFiles: Iterable[MutatedFile]): Unit = {
    val files = sourceCollector.filesToCopy(process)

    debug("Using temp directory: " + tmpDir)

    files.foreach(copyFile)

    // Overwrite files to mutated files
    mutatedFiles.foreach(writeMutatedFile)
  }

  private def copyFile(file: File): Unit = {
    val filePath = tmpDir / file.relativePath.toString

    filePath.createIfNotExists(file.isDirectory, createParents = true)

    file.copyTo(filePath, overwrite = true)
  }

  private def writeMutatedFile(mutatedFile: MutatedFile): File = {
    val subPath = mutatedFile.fileOrigin.relativePath
    val filePath = tmpDir / subPath.toString
    filePath.overwrite(mutatedFile.tree.syntax)
  }

  private def runMutants(mutatedFiles: Iterable[MutatedFile]): Iterable[MutantRunResult] = {
    val totalMutants = mutatedFiles.flatMap(_.mutants).size

    for {
      mutatedFile <- mutatedFiles
      subPath = mutatedFile.fileOrigin.relativePath
      mutant <- mutatedFile.mutants
    } yield {
      val result = runMutant(mutant, tmpDir)(subPath)
      val id = mutant.id + 1
      info(s"Finished mutation run $id/$totalMutants (${((id / totalMutants.toDouble) * 100).round}%)")
      result
    }
  }

  def runMutant(mutant: Mutant, workingDir: File): Path => MutantRunResult

}
