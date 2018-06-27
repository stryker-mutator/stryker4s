package stryker4s.sbt

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object Stryker4sPlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin

  object autoImport {
    val stryker = taskKey[Unit]("Run Stryker4s")
  }

  import autoImport._

  override def trigger = allRequirements

  override lazy val buildSettings = Seq(
    stryker := {
      val tests = (LocalRootProject / Test / definedTests).value
      val sbtRunner = new Stryker4sSbtRunner
      sbtRunner.run()
    }
  )
}
