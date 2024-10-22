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

  // TODO: revert when https://github.com/sbt/sbt/issues/7768 is released
  private val configValuesTupled = Def.setting(
    (
      strykerMutate.?.value,
      strykerBaseDir.?.value,
      strykerTestFilter.?.value,
      strykerReporters.?.value,
      strykerFiles.?.value,
      strykerExcludedMutations.?.value,
      strykerThresholdsHigh.?.value,
      strykerThresholdsLow.?.value,
      strykerThresholdsBreak.?.value,
      strykerDashboardBaseUrl.?.value,
      strykerDashboardReportType.?.value,
      strykerDashboardProject.?.value,
      strykerDashboardVersion.?.value,
      strykerDashboardModule.?.value,
      strykerTimeout.?.value,
      strykerTimeoutFactor.?.value
    )
  )

  def apply[F[_]]() = Def.setting[ConfigSource[F]] {
    val (
      mutateValue,
      baseDirValue,
      testFilterValue,
      reportersValue,
      filesValue,
      excludedMutationsValue,
      thresholdsHighValue,
      thresholdsLowValue,
      thresholdsBreakValue,
      dashboardBaseUrlValue,
      dashboardReportTypeValue,
      dashboardProjectValue,
      dashboardVersionValue,
      dashboardModuleValue,
      timeoutValue,
      timeoutFactorValue
    ) = configValuesTupled.value

    val maxTestRunnerReuseValue = strykerMaxTestRunnerReuse.?.value
    val legacyTestRunnerValue = strykerLegacyTestRunner.?.value
    val scalaDialectValue = strykerScalaDialect.?.value
    val concurrencyValue = strykerConcurrency.?.value
    val debugLogTestRunnerStdoutValue = strykerDebugLogTestRunnerStdout.?.value
    val debugDebugTestRunnerValue = strykerDebugDebugTestRunner.?.value
    val staticTmpDirValue = strykerStaticTmpDir.?.value
    val cleanTmpDirValue = strykerCleanTmpDir.?.value
    val openReportValue = strykerOpenReport.?.value

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
        mutateValue,
        strykerMutate.key.label
      )

      override val baseDir: ConfigValue[F, Path] = sbtSetting(
        baseDirValue,
        strykerBaseDir.key.label
      ).map(f => Path.fromNioPath(f.toPath.toAbsolutePath()))

      override def testFilter: ConfigValue[F, Seq[String]] = sbtSetting(
        testFilterValue,
        strykerTestFilter.key.label
      )

      override def reporters: ConfigValue[F, Seq[ReporterType]] = sbtSetting(
        reportersValue,
        strykerReporters.key.label
      ).as[Seq[ReporterType]]

      override def files: ConfigValue[F, Seq[String]] = sbtSetting(
        filesValue,
        strykerFiles.key.label
      )

      override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] = sbtSetting(
        excludedMutationsValue,
        strykerExcludedMutations.key.label
      ).as[Seq[ExcludedMutation]]

      override def thresholdsHigh: ConfigValue[F, Int] = sbtSetting(
        thresholdsHighValue,
        strykerThresholdsHigh.key.label
      )

      override def thresholdsLow: ConfigValue[F, Int] = sbtSetting(
        thresholdsLowValue,
        strykerThresholdsLow.key.label
      )

      override def thresholdsBreak: ConfigValue[F, Int] = sbtSetting(
        thresholdsBreakValue,
        strykerThresholdsBreak.key.label
      )

      override def dashboardBaseUrl: ConfigValue[F, Uri] = sbtSetting(
        dashboardBaseUrlValue,
        strykerDashboardBaseUrl.key.label
      ).as[Uri]

      override def dashboardReportType: ConfigValue[F, DashboardReportType] = sbtSetting(
        dashboardReportTypeValue,
        strykerDashboardReportType.key.label
      )

      override def dashboardProject: ConfigValue[F, Option[String]] = optSbtSetting(
        dashboardProjectValue,
        strykerDashboardProject.key.label
      )

      override def dashboardVersion: ConfigValue[F, Option[String]] = optSbtSetting(
        dashboardVersionValue,
        strykerDashboardVersion.key.label
      )

      override def dashboardModule: ConfigValue[F, Option[String]] = sbtSetting(
        dashboardModuleValue,
        strykerDashboardModule.key.label
      ).map(_.some)

      override def timeout: ConfigValue[F, FiniteDuration] = sbtSetting(
        timeoutValue,
        strykerTimeout.key.label
      )

      override def timeoutFactor: ConfigValue[F, Double] = sbtSetting(
        timeoutFactorValue,
        strykerTimeoutFactor.key.label
      )

      override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = sbtSetting(
        maxTestRunnerReuseValue,
        strykerMaxTestRunnerReuse.key.label
      ).map(_.some)

      override def legacyTestRunner: ConfigValue[F, Boolean] = sbtSetting(
        legacyTestRunnerValue,
        strykerLegacyTestRunner.key.label
      )

      override def scalaDialect: ConfigValue[F, Dialect] = sbtSetting(
        scalaDialectValue,
        strykerScalaDialect.key.label
      )

      override def concurrency: ConfigValue[F, Int] = sbtSetting(
        concurrencyValue,
        strykerConcurrency.key.label
      )

      override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] = sbtSetting(
        debugLogTestRunnerStdoutValue,
        strykerDebugLogTestRunnerStdout.key.label
      )

      override def debugDebugTestRunner: ConfigValue[F, Boolean] = sbtSetting(
        debugDebugTestRunnerValue,
        strykerDebugDebugTestRunner.key.label
      )

      override def staticTmpDir: ConfigValue[F, Boolean] = sbtSetting(
        staticTmpDirValue,
        strykerStaticTmpDir.key.label
      )

      override def cleanTmpDir: ConfigValue[F, Boolean] = sbtSetting(
        cleanTmpDirValue,
        strykerCleanTmpDir.key.label
      )

      override def openReport: ConfigValue[F, Boolean] = sbtSetting(
        openReportValue,
        strykerOpenReport.key.label
      )

      override def testRunnerCommand: ConfigValue[F, String] = notSupported
      override def testRunnerArgs: ConfigValue[F, String] = notSupported

    }
  }
}
