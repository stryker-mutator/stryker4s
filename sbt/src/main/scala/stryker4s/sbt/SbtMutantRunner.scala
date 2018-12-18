package stryker4s.sbt
import java.nio.file.Path

import better.files.File
import sbt.Keys._
import sbt._
import stryker4s.config.Config
import stryker4s.model._
import stryker4s.run.MutantRunner
import stryker4s.run.process.ProcessRunner

class SbtMutantRunner(state: State, processRunner: ProcessRunner)(implicit config: Config)
    extends MutantRunner(processRunner) {

  val extracted: Extracted = Project.extract(state)

  override def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult = {
    val newState = extracted.appendWithoutSession(settings(workingDir, mutant.id), state)

    Project.runTask(test in Test, newState) match {
      case None =>
        throw new RuntimeException(
          s"An unexpected error occurred while running mutation ${mutant.id}")
      case Some((_, Value(_))) => newState.exit(true); Survived(mutant, subPath.toString)
      case Some((_, Inc(_)))   => newState.exit(true); Killed(mutant, subPath.toString)
    }
  }

  private[this] def settings(tmpDir: File, mutation: Int): Seq[Def.Setting[_]] = {
    val mainPath = {
      extracted
        .get(Compile / scalaSource)
        .absolutePath
        .diff(
          extracted.get(Compile / baseDirectory).absolutePath
        )
    }

    val testPath = {
      extracted
        .get(Test / scalaSource)
        .absolutePath
        .diff(
          extracted.get(Test / baseDirectory).absolutePath
        )
    }
    SbtStateSettings.noLoggingSettings ++ Seq(
      // Set active mutation
      javaOptions in Test += s"-DACTIVE_MUTATION=${String.valueOf(mutation)}",

      scalaSource in Compile := tmpDir.toJava / mainPath,
      scalaSource in Test := tmpDir.toJava / testPath
    )
  }
}