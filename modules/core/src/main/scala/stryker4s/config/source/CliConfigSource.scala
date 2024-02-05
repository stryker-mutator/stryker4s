package stryker4s.config.source

import cats.syntax.all.*
import ciris.*
import fs2.io.file.Path
import stryker4s.config.codec.CirisConfigDecoders
import stryker4s.config.{ConfigOrder, DashboardReportType, ExcludedMutation, ReporterType}
import sttp.model.Uri

import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect

class CliConfigSource[F[_]](args: Seq[String]) extends ConfigSource[F] with CirisConfigDecoders {

  override def name: String = "CLI arguments"

  override def priority: ConfigOrder = ConfigOrder(5)

  private def findArg(name: String): ConfigValue[F, String] = {
    val key = ConfigKey(name)
    args.find(_.startsWith(s"--$name=")) match {
      case Some(value) => ConfigValue.loaded(key, value.drop(name.length + 3))
      case None        => ConfigValue.missing(s"cli arg --$name")
    }
  }

  private def splitArg(name: String): ConfigValue[F, Seq[String]] = findArg(name).map(_.split(",").toSeq)

  override def testFilter: ConfigValue[F, Seq[String]] = splitArg("test-filter")
  override def mutate: ConfigValue[F, Seq[String]] = splitArg("mutate")

  override def baseDir: ConfigValue[F, Path] = findArg("base-dir").as[Path]

  override def reporters: ConfigValue[F, Seq[ReporterType]] = splitArg("reporters").as[Seq[ReporterType]]

  override def files: ConfigValue[F, Seq[String]] = splitArg("files")

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] =
    splitArg("excluded-mutations").as[Seq[ExcludedMutation]]

  override def thresholdsHigh: ConfigValue[F, Int] = findArg("thresholds.high").as[Int]
  override def thresholdsLow: ConfigValue[F, Int] = findArg("thresholds.low").as[Int]
  override def thresholdsBreak: ConfigValue[F, Int] = findArg("thresholds.break").as[Int]

  override def dashboardBaseUrl: ConfigValue[F, Uri] = findArg("dashboard.base-url").as[Uri]
  override def dashboardReportType: ConfigValue[F, DashboardReportType] =
    findArg("dashboard.report-type").as[DashboardReportType]
  override def dashboardProject: ConfigValue[F, Option[String]] = findArg("dashboard.project").as[String].map(_.some)
  override def dashboardVersion: ConfigValue[F, Option[String]] = findArg("dashboard.version").as[String].map(_.some)
  override def dashboardModule: ConfigValue[F, Option[String]] = findArg("dashboard.module").as[String].map(_.some)

  override def timeout: ConfigValue[F, FiniteDuration] = findArg("timeout").as[FiniteDuration]

  override def timeoutFactor: ConfigValue[F, Double] = findArg("timeout-factor").as[Double]

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = findArg("max-test-runner-reuse").as[Int].map(_.some)

  override def legacyTestRunner: ConfigValue[F, Boolean] = findArg("legacy-test-runner").as[Boolean]

  override def scalaDialect: ConfigValue[F, Dialect] = findArg("scala-dialect").as[Dialect]

  override def concurrency: ConfigValue[F, Int] = findArg("concurrency").as[Int]

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] = findArg("debug.log-test-runner-stdout").as[Boolean]
  override def debugDebugTestRunner: ConfigValue[F, Boolean] = findArg("debug.debug-test-runner").as[Boolean]

  override def staticTmpDir: ConfigValue[F, Boolean] = findArg("static-tmp-dir").as[Boolean]

  override def cleanTmpDir: ConfigValue[F, Boolean] = findArg("clean-tmp-dir").as[Boolean]

}
