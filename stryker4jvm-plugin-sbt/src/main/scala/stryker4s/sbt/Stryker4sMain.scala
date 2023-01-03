package stryker4s.sbt

import cats.effect.unsafe.IORuntime
import cats.effect.{Deferred, IO}
import sbt.Keys.*
import sbt.*
import sbt.plugins.*
import stryker4s.log.{Logger, SbtLogger}
import stryker4s.run.threshold.ErrorStatus
import scala.concurrent.duration.FiniteDuration
import fs2.io.file

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
  import autoImport.*

  override lazy val projectSettings: Seq[Def.Setting[?]] = Seq(
    stryker := strykerTask.value,
    stryker / logLevel := Level.Info,
    stryker / onLoadMessage := "", // Prevents "[info] Set current project to ..." in between mutations
    strykerMinimumSbtVersion := "1.4.0",
    strykerIsSupported := sbtVersion.value >= strykerMinimumSbtVersion.value
  )

  lazy val strykerTask = Def.task {
    if (!strykerIsSupported.value) {
      throw new UnsupportedSbtVersionException(
        s"Sbt version ${sbtVersion.value} is not supported by Stryker4s. Please upgrade to a later version. The lowest supported version is ${strykerMinimumSbtVersion.value}. If you know what you are doing you can override this with the 'strykerIsSupported' sbt setting."
      )
    }
    // Call logLevel so it shows up as a used setting when set
    val _ = (stryker / logLevel).value

    implicit val runtime: IORuntime = IORuntime.global
    implicit val logger: Logger = new SbtLogger(streams.value.log)

    val sources =
      Seq((Compile / scalaSource).value, (Compile / javaSource).value).map(_.toPath()).map(file.Path.fromNioPath)
    val targetPath = file.Path.fromNioPath(target.value.toPath())

    Deferred[IO, FiniteDuration] // Create shared timeout between testrunners
      .map(new Stryker4sSbtRunner(state.value, _, sources, targetPath))
      .flatMap(_.run())
      .map {
        case ErrorStatus => throw new MessageOnlyException("Mutation score is below configured threshold")
        case _           => ()
      }
      .unsafeRunSync()
  }

  private class UnsupportedSbtVersionException(s: String)
      extends IllegalArgumentException(s)
      with FeedbackProvidedException
}
