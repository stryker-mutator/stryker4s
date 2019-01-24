package stryker4s.sbt

import sbt.Keys._
import sbt._
import sbt.plugins._
import stryker4s.run.threshold.ErrorStatus

/**
  * This plugin adds a new task (stryker) to the project that allow you to run mutation testing over your code
  */
object Stryker4sPlugin extends AutoPlugin {

  override def requires = JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    val stryker = taskKey[State]("Run Stryker4s")
  }
  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    stryker := strykerTask.value
  )

  val strykerTask = Def.task {
    // Run Stryker
    val currentState = state.value
    val result = new Stryker4sSbtRunner(currentState).run()

    result match {
      case ErrorStatus => throw new MessageOnlyException("Mutation score is below configured threshold")
      case _           => currentState
    }
  }
}
