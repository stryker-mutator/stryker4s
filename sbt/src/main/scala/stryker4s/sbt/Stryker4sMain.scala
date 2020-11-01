package stryker4s.sbt

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

import cats.effect.{ContextShift, IO => CatsIO, Timer}
import sbt.Keys._
import sbt._
import sbt.plugins._
import stryker4s.log.{Logger, SbtLogger}
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
    implicit val logger: Logger = new SbtLogger(streams.value.log)

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

  private class UnsupportedSbtVersionException(s: String)
      extends IllegalArgumentException(s)
      with FeedbackProvidedException
}
