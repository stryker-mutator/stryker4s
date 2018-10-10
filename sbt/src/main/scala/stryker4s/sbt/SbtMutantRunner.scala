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
  // TODO: improve scalaSource values
  //    val mainSource = extracted.get(Compile / scalaSource)
  //    val testSource = extracted.get(Test / scalaSource)
  private[this] def settings(tmpDir: File, mutation: Int) = Seq(
    scalaSource in Compile := (scalaSource in Compile).transform(file(tmpDir.pathAsString)),
    scalaSource in Test := tmpDir.toJava / "src" / "test" / "scala",
    fork in Test := true,
    javaOptions in Test := Seq(s"-DACTIVE_MUTATION=$mutation")
  )
}
