package stryker4s.run

import java.nio.file.Path

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.extensions.FileExtensions._
import stryker4s.extensions.score.MutationScoreCalculator
import stryker4s.model._
import stryker4s.run.process.{Command, ProcessRunner}

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ProcessMutantRunner(command: Command, process: ProcessRunner)(implicit config: Config)
    extends MutantRunner
    with MutationScoreCalculator
    with Logging {

  override def apply(files: Iterable[File], mutatedFiles: Iterable[MutatedFile]): MutantRunResults = {
    val startTime = System.currentTimeMillis()
    val tmpDir = File.newTemporaryDirectory("stryker4s-", Option(config.baseDir / "target"))
    debug("Using temp directory: " + tmpDir)

    val parentDirName = config.baseDir.name

    files.foreach(file => {

      val bla1 = file.tokens().takeWhile(!_.equals(parentDirName))



      val o = File(bla1.mkString("/"))

      val dir = config.baseDir.name
      val blabla = file.glob(config.baseDir.name)
      val bla = file.relativize(config.baseDir)


      file.copyToDirectory(tmpDir)
    })
    mutatedFiles.foreach(file => file.fileOrigin.copyTo(tmpDir))

    // Overwrite files to mutated files
    mutatedFiles foreach {
      case MutatedFile(file, tree, _) =>
        val subPath = file.relativePath
        val filePath = tmpDir / subPath.toString
        filePath.overwrite(tree.syntax)
    }

    val totalMutants = mutatedFiles.flatMap(_.mutants).size

    val runResults = for {
      mutatedFile <- mutatedFiles
      subPath = mutatedFile.fileOrigin.relativePath
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
}
