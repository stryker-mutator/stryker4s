package stryker4s.sbt

import better.files.File
import sbt.Keys.{fork, javaOptions, scalaSource, test}
import sbt.{Extracted, Inc, Project, State, Value}
import stryker4s.config.Config
import stryker4s.extensions.score.MutationScoreCalculator
import stryker4s.model._
import stryker4s.run.MutantRunner
import stryker4s.extensions.FileExtensions._
import java.nio.file.Path
import grizzled.slf4j.Logging
import sbt._

import scala.concurrent.duration.{Duration, MILLISECONDS}

/**
  * This Mutant Runner implementation mutates the original code and run Test / test over every mutation
  */
class SbtMutantRunner(state:State)(implicit config:Config) extends MutantRunner with MutationScoreCalculator with Logging {

  val extracted: Extracted = Project.extract(state)

  def apply(files: Iterable[MutatedFile]): MutantRunResults = {

    val startTime = java.lang.System.currentTimeMillis()
    val tmpDir = File.newTemporaryDirectory("stryker4s-")
    debug("Using temp directory: " + tmpDir)

    config.baseDir.copyTo(tmpDir)

    // Overwrite files to mutated files
    files foreach {
      case MutatedFile(file, tree, _) =>
        val subPath = file.relativePath
        val filePath = tmpDir / subPath.toString
        filePath.overwrite(tree.syntax)
    }

    val totalMutants = files.flatMap(_.mutants).size

    val runResults = for {
      mutatedFile <- files
      subPath = mutatedFile.fileOrigin.relativePath
      mutant <- mutatedFile.mutants
    } yield {
      val result = runMutant(mutant, tmpDir, subPath)
      val id = mutant.id
      info(
        s"Finished mutation run $id/$totalMutants (${((id / totalMutants.toDouble) * 100).round}%)")
      result
    }

    val duration = Duration(java.lang.System.currentTimeMillis() - startTime, MILLISECONDS)
    val detected = runResults collect { case d: Detected => d }

    MutantRunResults(runResults, calculateMutationScore(totalMutants, detected.size), duration)

  }

  private[this] def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult = {

    val newState = extracted.appendWithSession(settings(workingDir, mutant.id), state)

    Project.runTask(test in Test, newState) match {
      case None => throw new RuntimeException("this cannot happend ever")
      case Some((_, Value(_))) => Survived(mutant, subPath)
      case Some((_, Inc(_))) => Killed(0, mutant, subPath)
    }

  }

  // TODO: improve scalaSource values
  //    val mainSource = extracted.get(Compile / scalaSource)
  //    val testSource = extracted.get(Test / scalaSource)
  private[this] def settings(tmpDir:File, mutation:Int) = Seq(
    scalaSource in Compile := tmpDir.toJava / "src" / "main" / "scala",
    scalaSource in Test := tmpDir.toJava / "src" / "test" / "scala",
    fork in Test := true,
    javaOptions in Test := Seq(s"-DACTIVE_MUTATION=$mutation")
  )

}