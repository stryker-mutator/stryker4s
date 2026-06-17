package stryker4s.mill

import cats.syntax.option.*
import ciris.{ConfigKey, ConfigValue}
import fs2.io.file.Path
import stryker4s.config.codec.CirisConfigDecoders
import stryker4s.config.source.ConfigSource
import stryker4s.config.{ConfigOrder, DashboardReportType, ExcludedMutation, ReporterType}
import sttp.model.Uri

import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect

/** A [[stryker4s.config.source.ConfigSource]] of (pre-evaluated) values from the Mill build, provided by the `Stryker`
  * config defs on [[stryker4s.mill.Stryker4sModule]]
  */
class MillConfigSource[F[_]](
    baseDirValue: Path,
    mutateValue: Option[Seq[String]],
    filesValue: Option[Seq[String]],
    testFilterValue: Option[Seq[String]],
    reportersValue: Option[Seq[String]],
    excludedMutationsValue: Option[Seq[String]],
    thresholdsHighValue: Option[Int],
    thresholdsLowValue: Option[Int],
    thresholdsBreakValue: Option[Int],
    dashboardBaseUrlValue: Option[String],
    dashboardReportTypeValue: Option[String],
    dashboardProjectValue: Option[String],
    dashboardVersionValue: Option[String],
    dashboardModuleValue: Option[String],
    timeoutValue: Option[FiniteDuration],
    timeoutFactorValue: Option[Double],
    maxTestRunnerReuseValue: Option[Int],
    scalaDialectValue: Option[Dialect],
    concurrencyValue: Option[Int],
    debugLogTestRunnerStdoutValue: Option[Boolean],
    debugDebugTestRunnerValue: Option[Boolean],
    staticTmpDirValue: Option[Boolean],
    cleanTmpDirValue: Option[Boolean],
    openReportValue: Option[Boolean]
) extends ConfigSource[F]
    with CirisConfigDecoders {

  override def name: String = "mill build"

  override def priority: ConfigOrder = ConfigOrder(15)

  private def millKey(key: String): ConfigKey =
    ConfigKey(s"mill config $key")

  private def millValue[A](value: Option[A], key: String): ConfigValue[F, A] = value match {
    case None        => ConfigValue.missing(millKey(key))
    case Some(value) => ConfigValue.loaded(millKey(key), value)
  }

  override def mutate: ConfigValue[F, Seq[String]] = millValue(mutateValue, "strykerMutate")

  override def baseDir: ConfigValue[F, Path] =
    ConfigValue.loaded(millKey("moduleDir"), baseDirValue.absolute)

  override def testFilter: ConfigValue[F, Seq[String]] = millValue(testFilterValue, "strykerTestFilter")

  override def reporters: ConfigValue[F, Seq[ReporterType]] =
    millValue(reportersValue, "strykerReporters").as[Seq[ReporterType]]

  override def files: ConfigValue[F, Seq[String]] = millValue(filesValue, "strykerFiles")

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] =
    millValue(excludedMutationsValue, "strykerExcludedMutations").as[Seq[ExcludedMutation]]

  override def thresholdsHigh: ConfigValue[F, Int] = millValue(thresholdsHighValue, "strykerThresholdsHigh")
  override def thresholdsLow: ConfigValue[F, Int] = millValue(thresholdsLowValue, "strykerThresholdsLow")
  override def thresholdsBreak: ConfigValue[F, Int] = millValue(thresholdsBreakValue, "strykerThresholdsBreak")

  override def dashboardBaseUrl: ConfigValue[F, Uri] =
    millValue(dashboardBaseUrlValue, "strykerDashboardBaseUrl").as[Uri]

  override def dashboardReportType: ConfigValue[F, DashboardReportType] =
    millValue(dashboardReportTypeValue, "strykerDashboardReportType").as[DashboardReportType]

  override def dashboardProject: ConfigValue[F, Option[String]] =
    millValue(dashboardProjectValue, "strykerDashboardProject").map(_.some)

  override def dashboardVersion: ConfigValue[F, Option[String]] =
    millValue(dashboardVersionValue, "strykerDashboardVersion").map(_.some)

  override def dashboardModule: ConfigValue[F, Option[String]] =
    millValue(dashboardModuleValue, "strykerDashboardModule").map(_.some)

  override def timeout: ConfigValue[F, FiniteDuration] = millValue(timeoutValue, "strykerTimeout")

  override def timeoutFactor: ConfigValue[F, Double] = millValue(timeoutFactorValue, "strykerTimeoutFactor")

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] =
    millValue(maxTestRunnerReuseValue, "strykerMaxTestRunnerReuse").map(_.some)

  override def scalaDialect: ConfigValue[F, Dialect] = millValue(scalaDialectValue, "strykerScalaDialect")

  override def concurrency: ConfigValue[F, Int] = millValue(concurrencyValue, "strykerConcurrency")

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] =
    millValue(debugLogTestRunnerStdoutValue, "strykerDebugLogTestRunnerStdout")

  override def debugDebugTestRunner: ConfigValue[F, Boolean] =
    millValue(debugDebugTestRunnerValue, "strykerDebugDebugTestRunner")

  override def staticTmpDir: ConfigValue[F, Boolean] = millValue(staticTmpDirValue, "strykerStaticTmpDir")

  override def cleanTmpDir: ConfigValue[F, Boolean] = millValue(cleanTmpDirValue, "strykerCleanTmpDir")

  override def openReport: ConfigValue[F, Boolean] = millValue(openReportValue, "strykerOpenReport")

  // The legacy test runner is sbt-only
  override def legacyTestRunner: ConfigValue[F, Boolean] = notSupported

  override def testRunnerCommand: ConfigValue[F, String] = notSupported
  override def testRunnerArgs: ConfigValue[F, String] = notSupported

  override def showHelpMessage: ConfigValue[F, Option[String]] = notSupported
}
