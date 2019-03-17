package stryker4s.sbt

import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configurator
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
    logLevel in stryker := Level.Info,
    onLoadMessage := "" // Prevents "[info] Set current project to ..." inbetween mutations
  )

  val strykerTask = Def.task {
    // Run Stryker4s
    val currentState = state.value

    // Set the log level used for stryker log messages
    setStrykerLogLevel((logLevel in stryker).value)

    val result = new Stryker4sSbtRunner(currentState).run()

    result match {
      case ErrorStatus => throw new MessageOnlyException("Mutation score is below configured threshold")
      case _           => currentState
    }
  }

  private def sbtLogLevelToLog4j(level: Level.Value): org.apache.logging.log4j.Level =
    level match {
      case Level.Warn  => org.apache.logging.log4j.Level.WARN
      case Level.Error => org.apache.logging.log4j.Level.ERROR
      case Level.Debug => org.apache.logging.log4j.Level.DEBUG
      case _           => org.apache.logging.log4j.Level.INFO
    }

  private def setStrykerLogLevel(level: Level.Value): Unit = {
    val log4jLevel: org.apache.logging.log4j.Level = sbtLogLevelToLog4j(level)
    Configurator.setRootLevel(log4jLevel)
    LoggerContext.getContext(false).getRootLogger.info(s"Set stryker logging level to $log4jLevel")
  }
}
