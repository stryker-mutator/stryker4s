package stryker4s.sbt

import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.{Level => Log4jLevel}
import sbt.Keys._
import sbt._
import sbt.plugins._
import stryker4s.run.threshold.ErrorStatus

import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * This plugin adds a new task (stryker) to the project that allow you to run mutation testing over your code
  */
object Stryker4sMain extends AutoPlugin {
  override def requires = JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    val stryker = taskKey[State]("Run Stryker4s")
  }
  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    stryker := strykerTask.value,
    logLevel in stryker := Level.Info,
    onLoadMessage := "" // Prevents "[info] Set current project to ..." in between mutations
  )

  val strykerTask = Def.task {
    setStrykerLogLevel((logLevel in stryker).value)

    val currentState = state.value
    val result = new Stryker4sSbtRunner(currentState).run()

    result match {
      case ErrorStatus => throw new MessageOnlyException("Mutation score is below configured threshold")
      case _           => currentState
    }
  }

  private def setStrykerLogLevel(level: Level.Value): Unit = {
    Configurator.setRootLevel(level)
    LoggerContext.getContext(false).getRootLogger.info(s"Set stryker4s logging level to $level")
  }

  implicit private[this] def toLog4jLogLevel(level: Level.Value): Log4jLevel =
    level match {
      case Level.Warn  => Log4jLevel.WARN
      case Level.Error => Log4jLevel.ERROR
      case Level.Debug => Log4jLevel.DEBUG
      case _           => Log4jLevel.INFO
    }
}
