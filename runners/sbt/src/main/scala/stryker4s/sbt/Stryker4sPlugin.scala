package stryker4s.sbt

import sbt.Keys._
import sbt._
import sbt.plugins._
import stryker4s.run.threshold.ErrorStatus

/**
  * This plugin adds a new command (stryker) to the project that allow you to run stryker mutation over your code
  */
object Stryker4sPlugin extends AutoPlugin {

  override def requires = JvmPlugin
  override def trigger = allRequirements

  object autoImport {}

  lazy val strykerDefaultSettings: Seq[Def.Setting[_]] = Seq(
    commands += stryker
  )

  def stryker: Command = Command.command("stryker") { currentState =>
    // Run Stryker
    val result = new Stryker4sSbtRunner(currentState).run()

    result match {
      case ErrorStatus => currentState.fail
      case _           => currentState
    }

  }

  override lazy val projectSettings: Seq[Def.Setting[_]] = strykerDefaultSettings

}
