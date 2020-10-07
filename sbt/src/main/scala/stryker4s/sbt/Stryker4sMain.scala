package stryker4s.sbt

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

import cats.effect.{ContextShift, IO => CatsIO, Timer}
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.{Level => Log4jLevel}
import sbt.Keys._
import sbt._
import sbt.plugins._
import stryker4s.run.threshold.ErrorStatus

/** This plugin adds a new task (stryker) to the project that allow you to run mutation testing over your code
  */
object Stryker4sMain extends AutoPlugin {
  override def requires = JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    val stryker = taskKey[Unit]("Run Stryker4s mutation testing")
    val strykerMinimumSbtVersion = settingKey[String]("Lowest supported sbt version by Stryker4s")
    val strykerIsSupported = settingKey[Boolean]("If running Stryker4s is supported on this sbt version")
  }
  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    stryker := strykerTask.value,
    logLevel in stryker := Level.Info,
    onLoadMessage in stryker := "", // Prevents "[info] Set current project to ..." in between mutations
    strykerMinimumSbtVersion := "1.1.1",
    strykerIsSupported := sbtVersion.value >= strykerMinimumSbtVersion.value
  )

  lazy val strykerTask = Def.taskDyn[Unit] {
    if (strykerIsSupported.value)
      strykerImpl
    else
      strykerIsNotSupported
  }

  lazy val strykerImpl = Def.task {
    implicit val cs: ContextShift[CatsIO] = CatsIO.contextShift(implicitly[ExecutionContext])
    implicit val timer: Timer[CatsIO] = CatsIO.timer(implicitly[ExecutionContext])
    setStrykerLogLevel((logLevel in stryker).value)

    new Stryker4sSbtRunner(state.value)
      .run()
      .map {
        case ErrorStatus => throw new MessageOnlyException("Mutation score is below configured threshold")
        case _           => ()
      }
      .unsafeRunSync()
  }

  private lazy val strykerIsNotSupported: Def.Initialize[Task[Unit]] = Def.task {
    // Put in Unit def to prevent dead code warning
    def throwVersionException(): Unit =
      throw new UnsupportedSbtVersionException(
        s"Sbt version ${sbtVersion.value} is not supported by Stryker4s. Please upgrade to a later version. The lowest supported version is ${strykerMinimumSbtVersion.value}. If you know what you are doing you can override this with the 'strykerIsSupported' sbt setting."
      )
    throwVersionException()
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

  private class UnsupportedSbtVersionException(s: String)
      extends IllegalArgumentException(s)
      with FeedbackProvidedException
}
