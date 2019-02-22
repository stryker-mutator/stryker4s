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
    stryker := strykerTask.value,
    stryker := strykerTaskWrapper.value
  )

  val strykerTaskWrapper = Def.task {
    // Handle stryker task result
    stryker.result.value match {
      case Inc(inc: Incomplete) =>
        // Stryker failed, log the exception if it's available
        if (inc.directCause.isDefined) {
          sLog.value.error(s"Stryker failed with ${inc.directCause.get}")
          throw inc.directCause.get
        } else {
          sLog.value.error(s"Stryker failed for an unknown reason. :(")
          throw new Exception("Unknown failure")
        }
      case _ => state.value // Stryker succeeded, do nothing
    }
  }

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
