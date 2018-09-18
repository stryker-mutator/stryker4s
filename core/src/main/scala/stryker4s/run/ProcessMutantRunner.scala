package stryker4s.run

import java.nio.file.Path

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.model._
import stryker4s.run.process.{Command, ProcessRunner}

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ProcessMutantRunner(command: Command, process: ProcessRunner)(implicit config: Config)
    extends MutantRunner
    with Logging {

  override def apply(files: Iterable[MutatedFile]): MutantRunResults = {
    val startTime = System.currentTimeMillis()
    val tmpDir = File.newTemporaryDirectory("stryker4s-")
    debug("Using temp directory: " + tmpDir)

    config.baseDir.copyTo(tmpDir)

    // Overwrite files to mutated files
    files foreach {
      case MutatedFile(file, tree, _) =>
        val subPath = config.baseDir.relativize(file)
        val filePath = tmpDir / subPath.toString
        filePath.overwrite(tree.syntax)
    }

    val totalMutants = files.flatMap(_.mutants).size

    val runResults = for {
      mutatedFile <- files
      subPath = config.baseDir.relativize(mutatedFile.fileOrigin)
      mutant <- mutatedFile.mutants
    } yield {
      val result = runMutant(mutant, tmpDir, subPath)
      val id = mutant.id
      info(
        s"Finished mutation run $id/$totalMutants (${((id / totalMutants.toDouble) * 100).round}%)")
      result
    }

    val duration = Duration(System.currentTimeMillis() - startTime, MILLISECONDS)
    val detected = runResults collect { case d: Detected => d }

    MutantRunResults(runResults, calculateMutationScore(totalMutants, detected.size), duration)
  }

  private[this] def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult = {
    val id = mutant.id
    info(s"Starting test-run $id...")
    process(command, workingDir, ("ACTIVE_MUTATION", id.toString)) match {
      case Success(exitCode) if exitCode == 0 => Survived(mutant, subPath)
      case Success(exitCode)                  => Killed(exitCode, mutant, subPath)
      case Failure(exc: TimeoutException)     => TimedOut(exc, mutant, subPath)
    }
  }

  private[this] def calculateMutationScore(totalMutants: Double,
                                           detectedMutants: Double): Double = {
    val mutationScore = detectedMutants / totalMutants * 100
    BigDecimal(mutationScore).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
}
