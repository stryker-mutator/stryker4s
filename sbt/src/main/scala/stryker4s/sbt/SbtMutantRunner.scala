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
    val newState = extracted.appendWithSession(settings(workingDir, mutant.id), state)
    Project.runTask(test in Test, newState) match {
      case None                => throw new RuntimeException("this cannot happend ever")
      case Some((_, Value(_))) => Survived(mutant, subPath)
      case Some((_, Inc(_)))   => Killed(mutant, subPath)
    }
  }

  private[this] def settings(tmpDir: File, mutation: Int) = {

    val mainPath = {
      extracted.get(Compile / scalaSource).absolutePath.diff(
        extracted.get(Compile / baseDirectory).absolutePath
      )
    }

    val testPath = {
      extracted.get(Test / scalaSource).absolutePath.diff(
        extracted.get(Test / baseDirectory).absolutePath
      )
    }

    Seq(
      scalaSource in Compile := tmpDir.toJava / mainPath,
      scalaSource in Test := tmpDir.toJava / testPath,
      fork in Test := true,
      javaOptions in Test := Seq(s"-DACTIVE_MUTATION=$mutation")
    )

  }

}
