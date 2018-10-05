package stryker4s.sbt

import better.files.File
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import stryker4s.Stryker4s
import stryker4s.config.Config

object Stryker4sPlugin extends AutoPlugin {

  override def requires = JvmPlugin
  override def trigger  = allRequirements

  object autoImport {

    // Settings
    val strykerMutate = settingKey[Seq[String]]("Subset of files to use for mutation testing")
    val strykerLogLevel = settingKey[String]("Logging level")
    val strykerReporters = settingKey[Seq[String]]("Reporters for stryker4s to use")

  }

  import autoImport._

  lazy val strykerDefaultSettings: Seq[Def.Setting[_]] = Seq(
    commands += stryker
  )

  def stryker = Command.command("stryker") { currentState =>

    val extracted: Extracted = Project.extract(currentState)

    val tmpDir = File.newTemporaryDirectory("stryker4s-")

    val baseDir = extracted.get(LocalRootProject / baseDirectory)
    val mainSource = extracted.get(Compile / scalaSource)
    val testSource = extracted.get(Test / scalaSource)

    println(tmpDir)
    println(baseDir)
    println(mainSource)
    println(testSource)

    println("RUNNING STRYKER COMMAND")

    currentState
  }

  override lazy val projectSettings = strykerDefaultSettings


//  override lazy val buildSettings = Seq(
//    stryker := {
//      val tests = (LocalRootProject / Test / definedTests).value
//      val sbtRunner = new Stryker4sSbtRunner
//      sbtRunner.run()
//    }
//  )

}
