package stryker4s.config.source

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.all.*
import ciris.*
import fansi.{Color, Underlined}
import fs2.io.file.Path
import stryker4s.config.codec.CirisConfigDecoders
import stryker4s.config.{ConfigOrder, DashboardReportType, ExcludedMutation, ReporterType}
import stryker4s.log.Logger
import sttp.model.Uri

import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect

class AggregateConfigSource[F[_]: Sync](sources: NonEmptyList[ConfigSource[F]])(implicit log: Logger)
    extends ConfigSource[F]
    with CirisConfigDecoders {

  override def name: String = "aggregate"

  override def priority: ConfigOrder = ConfigOrder(0)

  override def testFilter: ConfigValue[F, Seq[String]] = loadAndLog("testFilter", _.testFilter)
  override def mutate: ConfigValue[F, Seq[String]] = loadAndLog("mutate", _.mutate)

  override def baseDir: ConfigValue[F, Path] = loadAndLog("baseDir", _.baseDir)

  override def reporters: ConfigValue[F, Seq[ReporterType]] = loadAndLog("reporters", _.reporters)

  override def files: ConfigValue[F, Seq[String]] = loadAndLog("files", _.files)

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] =
    loadAndLog("excludedMutations", _.excludedMutations)

  override def thresholdsHigh: ConfigValue[F, Int] = loadAndLog("thresholds.high", _.thresholdsHigh)
  override def thresholdsLow: ConfigValue[F, Int] = loadAndLog("thresholds.low", _.thresholdsLow)
  override def thresholdsBreak: ConfigValue[F, Int] = loadAndLog("thresholds.break", _.thresholdsBreak)

  override def dashboardBaseUrl: ConfigValue[F, Uri] = loadAndLog("dashboard.baseUrl", _.dashboardBaseUrl)
  override def dashboardReportType: ConfigValue[F, DashboardReportType] =
    loadAndLog("dashboardReportType", _.dashboardReportType)
  override def dashboardProject: ConfigValue[F, Option[String]] = loadAndLog("dashboard.project", _.dashboardProject)
  override def dashboardVersion: ConfigValue[F, Option[String]] = loadAndLog("dashboard.version", _.dashboardVersion)
  override def dashboardModule: ConfigValue[F, Option[String]] = loadAndLog("dashboard.module", _.dashboardModule)

  override def timeout: ConfigValue[F, FiniteDuration] = loadAndLog("timeout", _.timeout)

  override def timeoutFactor: ConfigValue[F, Double] = loadAndLog("timeoutFactor", _.timeoutFactor)

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = loadAndLog("maxTestRunnerReuse", _.maxTestRunnerReuse)

  override def legacyTestRunner: ConfigValue[F, Boolean] = loadAndLog("legacyTestRunner", _.legacyTestRunner)

  override def scalaDialect: ConfigValue[F, Dialect] = loadAndLog("scalaDialect", _.scalaDialect)

  override def concurrency: ConfigValue[F, Int] = loadAndLog("concurrency", _.concurrency)

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] =
    loadAndLog("debug.logTestRunnerStdout", _.debugLogTestRunnerStdout)
  override def debugDebugTestRunner: ConfigValue[F, Boolean] =
    loadAndLog("debug.debugTestRunner", _.debugDebugTestRunner)

  override def staticTmpDir: ConfigValue[F, Boolean] = loadAndLog("staticTmpDir", _.staticTmpDir)

  override def cleanTmpDir: ConfigValue[F, Boolean] = loadAndLog("cleanTmpDir", _.cleanTmpDir)

  /** Load a value from the sources, using the first available value
    */
  private def loadAndLog[A](name: String, f: ConfigSource[F] => ConfigValue[F, A]): ConfigValue[F, A] = sources
    .map(source =>
      f(source).evalMap(value =>
        Sync[F]
          .delay(log.debug(s"Loaded ${Color.Magenta(name)} from ${Underlined.On(source.name)}: $value"))
          .as(value)
      )
    )
    .reduceLeft(_ or _)

}
