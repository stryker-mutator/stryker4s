package stryker4s.config.source

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.all.*
import ciris.*
import fansi.Color
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

  override def testFilter: ConfigValue[F, Seq[String]] = loadAndLog(_.testFilter)
  override def mutate: ConfigValue[F, Seq[String]] = loadAndLog(_.mutate)

  override def baseDir: ConfigValue[F, Path] = loadAndLog(_.baseDir)

  override def reporters: ConfigValue[F, Seq[ReporterType]] = loadAndLog(_.reporters)

  override def files: ConfigValue[F, Seq[String]] = loadAndLog(_.files)

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] =
    loadAndLog(_.excludedMutations)

  override def thresholdsHigh: ConfigValue[F, Int] = loadAndLog(_.thresholdsHigh)
  override def thresholdsLow: ConfigValue[F, Int] = loadAndLog(_.thresholdsLow)
  override def thresholdsBreak: ConfigValue[F, Int] = loadAndLog(_.thresholdsBreak)

  override def dashboardBaseUrl: ConfigValue[F, Uri] = loadAndLog(_.dashboardBaseUrl)
  override def dashboardReportType: ConfigValue[F, DashboardReportType] =
    loadAndLog(_.dashboardReportType)
  override def dashboardProject: ConfigValue[F, Option[String]] = loadAndLog(_.dashboardProject)
  override def dashboardVersion: ConfigValue[F, Option[String]] = loadAndLog(_.dashboardVersion)
  override def dashboardModule: ConfigValue[F, Option[String]] = loadAndLog(_.dashboardModule)

  override def timeout: ConfigValue[F, FiniteDuration] = loadAndLog(_.timeout)

  override def timeoutFactor: ConfigValue[F, Double] = loadAndLog(_.timeoutFactor)

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = loadAndLog(_.maxTestRunnerReuse)

  override def legacyTestRunner: ConfigValue[F, Boolean] = loadAndLog(_.legacyTestRunner)

  override def scalaDialect: ConfigValue[F, Dialect] = loadAndLog(_.scalaDialect)

  override def concurrency: ConfigValue[F, Int] = loadAndLog(_.concurrency)

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] =
    loadAndLog(_.debugLogTestRunnerStdout)
  override def debugDebugTestRunner: ConfigValue[F, Boolean] =
    loadAndLog(_.debugDebugTestRunner)

  override def staticTmpDir: ConfigValue[F, Boolean] = loadAndLog(_.staticTmpDir)

  override def cleanTmpDir: ConfigValue[F, Boolean] = loadAndLog(_.cleanTmpDir)

  override def testRunnerCommand: ConfigValue[F, String] = loadAndLog(_.testRunnerCommand)
  override def testRunnerArgs: ConfigValue[F, String] = loadAndLog(_.testRunnerArgs)
  override def openReportAutomatically: ConfigValue[F, Boolean] = loadAndLog(_.openReportAutomatically)

  /** Load a value from the sources, using the first available value
    */
  private def loadAndLog[A](
      configValueFn: ConfigSource[F] => ConfigValue[F, A]
  )(implicit name: sourcecode.Name): ConfigValue[F, A] =
    sources
      .map(source =>
        configValueFn(source).evalMap(value =>
          Sync[F]
            .delay(log.debug(s"Loaded ${Color.Magenta(name.value)} from ${Color.Cyan(source.name)}: $value"))
            .as(value)
        )
      )
      .reduceLeft(_ or _)

}
