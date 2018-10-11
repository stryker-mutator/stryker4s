package stryker4s.sbt

import sbt.Keys._
import sbt._
import sbt.plugins._

/**
  * This plugin adds a new command (stryker) to the project that allow you to run stryker mutation over your code
  */
object Stryker4sPlugin extends AutoPlugin {

  override def requires = JvmPlugin
  override def trigger = allRequirements

  object autoImport {

    // Settings
    val strykerMutate = settingKey[Seq[String]]("Subset of files to use for mutation testing")
    val strykerLogLevel = settingKey[String]("Logging level")
    val strykerReporters = settingKey[Seq[String]]("Reporters for stryker4s to use")

  }

  lazy val strykerDefaultSettings: Seq[Def.Setting[_]] = Seq(
    commands += stryker
  )

  def stryker = Command.command("stryker") { currentState =>

    new Stryker4sSbtRunner(currentState).run()

    // After running state doesn't change
    currentState

  }

  // TODO: improve
  override lazy val projectSettings = strykerDefaultSettings

}
