package stryker4s.sbt

import cats.effect.IO
import cats.syntax.option.*
import ciris.{ConfigKey, ConfigValue}
import fs2.io.file.Path
import sbt.Def
import stryker4s.config.codec.CirisConfigDecoders
import stryker4s.config.source.ConfigSource
import stryker4s.config.{ConfigOrder, DashboardReportType, ExcludedMutation, ReporterType}
import stryker4s.sbt.Stryker4sPlugin.autoImport.*
import sttp.model.Uri

import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect

object SbtConfigSource {
  def apply(): Def.Initialize[ConfigSource[IO]] = Def.setting {
    new ConfigSource[IO] with CirisConfigDecoders {

      override def name: String = "sbt settings"

      override def priority: ConfigOrder = ConfigOrder(15)

      def sbtKey(key: String): ConfigKey =
        ConfigKey(s"sbt setting $key")

      def sbtSetting[A](value: Option[A], key: String): ConfigValue[IO, A] = value match {
        case None        => ConfigValue.missing(sbtKey(key))
        case Some(value) => ConfigValue.loaded(sbtKey(key), value)
      }

      def optSbtSetting[A](value: Option[Option[A]], key: String): ConfigValue[IO, Option[A]] = value match {
        case Some(Some(value)) => ConfigValue.loaded(sbtKey(key), value.some)
        case _                 => ConfigValue.missing(sbtKey(key))
      }

      override val mutate: ConfigValue[IO, Seq[String]] = sbtSetting(
        strykerMutate.?.value,
        strykerMutate.key.label
      )

      override val baseDir: ConfigValue[IO, Path] = sbtSetting(
        strykerBaseDir.?.value,
        strykerBaseDir.key.label
      ).map(f => Path.fromNioPath(f.toPath))

      override def testFilter: ConfigValue[IO, Seq[String]] = sbtSetting(
        strykerTestFilter.?.value,
        strykerTestFilter.key.label
      )

      override def reporters: ConfigValue[IO, Seq[ReporterType]] = sbtSetting(
        strykerReporters.?.value,
        strykerReporters.key.label
      ).as[Seq[ReporterType]]

      override def files: ConfigValue[IO, Seq[String]] = sbtSetting(
        strykerFiles.?.value,
        strykerFiles.key.label
      )

      override def excludedMutations: ConfigValue[IO, Seq[ExcludedMutation]] = sbtSetting(
        strykerExcludedMutations.?.value,
        strykerExcludedMutations.key.label
      ).as[Seq[ExcludedMutation]]

      override def thresholdsHigh: ConfigValue[IO, Int] = sbtSetting(
        strykerThresholdsHigh.?.value,
        strykerThresholdsHigh.key.label
      )
      override def thresholdsLow: ConfigValue[IO, Int] = sbtSetting(
        strykerThresholdsLow.?.value,
        strykerThresholdsLow.key.label
      )

      override def thresholdsBreak: ConfigValue[IO, Int] = sbtSetting(
        strykerThresholdsBreak.?.value,
        strykerThresholdsBreak.key.label
      )

      override def dashboardBaseUrl: ConfigValue[IO, Uri] = sbtSetting(
        strykerDashboardBaseUrl.?.value,
        strykerDashboardBaseUrl.key.label
      ).as[Uri]

      override def dashboardReportType: ConfigValue[IO, DashboardReportType] = sbtSetting(
        strykerDashboardReportType.?.value,
        strykerDashboardReportType.key.label
      )

      override def dashboardProject: ConfigValue[IO, Option[String]] = optSbtSetting(
        strykerDashboardProject.?.value,
        strykerDashboardProject.key.label
      )

      override def dashboardVersion: ConfigValue[IO, Option[String]] = optSbtSetting(
        strykerDashboardVersion.?.value,
        strykerDashboardVersion.key.label
      )

      override def dashboardModule: ConfigValue[IO, Option[String]] = sbtSetting(
        strykerDashboardModule.?.value,
        strykerDashboardModule.key.label
      ).map(_.some)

      override def timeout: ConfigValue[IO, FiniteDuration] = sbtSetting(
        strykerTimeout.?.value,
        strykerTimeout.key.label
      )

      override def timeoutFactor: ConfigValue[IO, Double] = sbtSetting(
        strykerTimeoutFactor.?.value,
        strykerTimeoutFactor.key.label
      )

      override def maxTestRunnerReuse: ConfigValue[IO, Option[Int]] = sbtSetting(
        strykerMaxTestRunnerReuse.?.value,
        strykerMaxTestRunnerReuse.key.label
      ).map(_.some)

      override def legacyTestRunner: ConfigValue[IO, Boolean] = sbtSetting(
        strykerLegacyTestRunner.?.value,
        strykerLegacyTestRunner.key.label
      )

      override def scalaDialect: ConfigValue[IO, Dialect] = sbtSetting(
        strykerScalaDialect.?.value,
        strykerScalaDialect.key.label
      )

      override def concurrency: ConfigValue[IO, Int] = sbtSetting(
        strykerConcurrency.?.value,
        strykerConcurrency.key.label
      )

      override def debugLogTestRunnerStdout: ConfigValue[IO, Boolean] = sbtSetting(
        strykerDebugLogTestRunnerStdout.?.value,
        strykerDebugLogTestRunnerStdout.key.label
      )
      override def debugDebugTestRunner: ConfigValue[IO, Boolean] = sbtSetting(
        strykerDebugDebugTestRunner.?.value,
        strykerDebugDebugTestRunner.key.label
      )

      override def staticTmpDir: ConfigValue[IO, Boolean] = sbtSetting(
        strykerStaticTmpDir.?.value,
        strykerStaticTmpDir.key.label
      )

      override def cleanTmpDir: ConfigValue[IO, Boolean] = sbtSetting(
        strykerCleanTmpDir.?.value,
        strykerCleanTmpDir.key.label
      )

    }
  }
}
