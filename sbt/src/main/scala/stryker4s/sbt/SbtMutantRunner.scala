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
  var generatedState: State = null

  var killed: Killed = null

  override def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult = {
    if(generatedState == null) generatedState = extracted.appendWithoutSession(settings(workingDir, mutant.id), state)

    if(generatedState == null) killed = Killed(mutant, subPath)

//    sys.props.put("ACTIVE_MUTATION", String.valueOf(mutant.id))

//    Project.runTask(test in Test, generatedState) match {
//      case None =>
//        throw new RuntimeException(
//          s"An unexpected error occurred while running mutation ${mutant.id}")
//      case Some((state, Value(_))) => {
//        generatedState = state
//        Survived(mutant, subPath)
//      }
//      case Some((state, Inc(_)))   => {
//        println("Exiting...")
//        generatedState = state
//        Killed(mutant, subPath)
//      }
//    }
    killed

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

    // Set active mutation
    sys.props.put("ACTIVE_MUTATION", String.valueOf(mutation))
    Seq(
      scalaSource in Compile := tmpDir.toJava / mainPath,
      scalaSource in Test := tmpDir.toJava / testPath
    )
  }
}
