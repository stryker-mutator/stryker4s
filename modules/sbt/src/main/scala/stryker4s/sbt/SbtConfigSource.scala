package stryker4s.sbt

import cats.syntax.option.*
import ciris.{ConfigKey, ConfigValue}
import fs2.io.file.Path
import sbt.Def
import stryker4s.config.codec.CirisConfigDecoders
import stryker4s.config.source.ConfigSource
import stryker4s.config.{ConfigOrder, DashboardReportType, ExcludedMutation, ReporterType}
import sttp.model.Uri

import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect

import Stryker4sPlugin.autoImport.*

object SbtConfigSource {
  def apply[F[_]]() = Def.setting[ConfigSource[F]] {
    new ConfigSource[F] with CirisConfigDecoders {

      override def name: String = "sbt settings"

      override def priority: ConfigOrder = ConfigOrder(15)

      def sbtKey(key: String): ConfigKey =
        ConfigKey(s"sbt setting $key")

      def sbtSetting[A](value: Option[A], key: String): ConfigValue[F, A] = value match {
        case None        => ConfigValue.missing(sbtKey(key))
        case Some(value) => ConfigValue.loaded(sbtKey(key), value)
      }

      def optSbtSetting[A](value: Option[Option[A]], key: String): ConfigValue[F, Option[A]] = value match {
        case Some(Some(value)) => ConfigValue.loaded(sbtKey(key), value.some)
        case _                 => ConfigValue.missing(sbtKey(key))
      }

      override val mutate: ConfigValue[F, Seq[String]] = sbtSetting(
        strykerMutate.?.value,
        strykerMutate.key.label
      )

      override val baseDir: ConfigValue[F, Path] = sbtSetting(
        strykerBaseDir.?.value,
        strykerBaseDir.key.label
      ).map(f => Path.fromNioPath(f.toPath.toAbsolutePath()))

      override def testFilter: ConfigValue[F, Seq[String]] = sbtSetting(
        strykerTestFilter.?.value,
        strykerTestFilter.key.label
      )

      override def reporters: ConfigValue[F, Seq[ReporterType]] = sbtSetting(
        strykerReporters.?.value,
        strykerReporters.key.label
      ).as[Seq[ReporterType]]

      override def files: ConfigValue[F, Seq[String]] = sbtSetting(
        strykerFiles.?.value,
        strykerFiles.key.label
      )

      override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] = sbtSetting(
        strykerExcludedMutations.?.value,
        strykerExcludedMutations.key.label
      ).as[Seq[ExcludedMutation]]

      override def thresholdsHigh: ConfigValue[F, Int] = sbtSetting(
        strykerThresholdsHigh.?.value,
        strykerThresholdsHigh.key.label
      )
      override def thresholdsLow: ConfigValue[F, Int] = sbtSetting(
        strykerThresholdsLow.?.value,
        strykerThresholdsLow.key.label
      )

      override def thresholdsBreak: ConfigValue[F, Int] = sbtSetting(
        strykerThresholdsBreak.?.value,
        strykerThresholdsBreak.key.label
      )

      override def dashboardBaseUrl: ConfigValue[F, Uri] = sbtSetting(
        strykerDashboardBaseUrl.?.value,
        strykerDashboardBaseUrl.key.label
      ).as[Uri]

      override def dashboardReportType: ConfigValue[F, DashboardReportType] = sbtSetting(
        strykerDashboardReportType.?.value,
        strykerDashboardReportType.key.label
      )

      override def dashboardProject: ConfigValue[F, Option[String]] = optSbtSetting(
        strykerDashboardProject.?.value,
        strykerDashboardProject.key.label
      )

      override def dashboardVersion: ConfigValue[F, Option[String]] = optSbtSetting(
        strykerDashboardVersion.?.value,
        strykerDashboardVersion.key.label
      )

      override def dashboardModule: ConfigValue[F, Option[String]] = sbtSetting(
        strykerDashboardModule.?.value,
        strykerDashboardModule.key.label
      ).map(_.some)

      override def timeout: ConfigValue[F, FiniteDuration] = sbtSetting(
        strykerTimeout.?.value,
        strykerTimeout.key.label
      )

      override def timeoutFactor: ConfigValue[F, Double] = sbtSetting(
        strykerTimeoutFactor.?.value,
        strykerTimeoutFactor.key.label
      )

      override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = sbtSetting(
        strykerMaxTestRunnerReuse.?.value,
        strykerMaxTestRunnerReuse.key.label
      ).map(_.some)

      override def legacyTestRunner: ConfigValue[F, Boolean] = sbtSetting(
        strykerLegacyTestRunner.?.value,
        strykerLegacyTestRunner.key.label
      )

      override def scalaDialect: ConfigValue[F, Dialect] = sbtSetting(
        strykerScalaDialect.?.value,
        strykerScalaDialect.key.label
      )

      override def concurrency: ConfigValue[F, Int] = sbtSetting(
        strykerConcurrency.?.value,
        strykerConcurrency.key.label
      )

      override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] = sbtSetting(
        strykerDebugLogTestRunnerStdout.?.value,
        strykerDebugLogTestRunnerStdout.key.label
      )
      override def debugDebugTestRunner: ConfigValue[F, Boolean] = sbtSetting(
        strykerDebugDebugTestRunner.?.value,
        strykerDebugDebugTestRunner.key.label
      )

      override def staticTmpDir: ConfigValue[F, Boolean] = sbtSetting(
        strykerStaticTmpDir.?.value,
        strykerStaticTmpDir.key.label
      )

      override def cleanTmpDir: ConfigValue[F, Boolean] = sbtSetting(
        strykerCleanTmpDir.?.value,
        strykerCleanTmpDir.key.label
      )

      override def openReport: ConfigValue[F, Boolean] = sbtSetting(
        strykerOpenReport.?.value,
        strykerOpenReport.key.label
      )

      override def testRunnerCommand: ConfigValue[F, String] = notSupported
      override def testRunnerArgs: ConfigValue[F, String] = notSupported

      override def showHelpMessage: ConfigValue[F, Option[String]] = notSupported
    }
  }
}
