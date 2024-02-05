package stryker4s.config.source

import cats.data.NonEmptyList
import cats.effect.Async
import cats.syntax.all.*
import ciris.*
import fs2.io.file.{Files, Path}
import stryker4s.config.{ConfigOrder, DashboardReportType, ExcludedMutation, ReporterType}
import stryker4s.log.Logger
import sttp.model.Uri

import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect

trait ConfigSource[+F[_]] {

  def name: String

  def priority: ConfigOrder

  def mutate: ConfigValue[F, Seq[String]]
  def testFilter: ConfigValue[F, Seq[String]]
  def baseDir: ConfigValue[F, Path]
  def reporters: ConfigValue[F, Seq[ReporterType]]
  def files: ConfigValue[F, Seq[String]]
  def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]]

  def thresholdsHigh: ConfigValue[F, Int]
  def thresholdsLow: ConfigValue[F, Int]
  def thresholdsBreak: ConfigValue[F, Int]

  def dashboardBaseUrl: ConfigValue[F, Uri]
  def dashboardReportType: ConfigValue[F, DashboardReportType]
  def dashboardProject: ConfigValue[F, Option[String]]
  def dashboardVersion: ConfigValue[F, Option[String]]
  def dashboardModule: ConfigValue[F, Option[String]]

  def timeout: ConfigValue[F, FiniteDuration]
  def timeoutFactor: ConfigValue[F, Double]
  def maxTestRunnerReuse: ConfigValue[F, Option[Int]]
  def legacyTestRunner: ConfigValue[F, Boolean]
  def scalaDialect: ConfigValue[F, Dialect]
  def concurrency: ConfigValue[F, Int]

  def debugLogTestRunnerStdout: ConfigValue[F, Boolean]
  def debugDebugTestRunner: ConfigValue[F, Boolean]

  def staticTmpDir: ConfigValue[F, Boolean]
  def cleanTmpDir: ConfigValue[F, Boolean]
}

object ConfigSource {

  /** Aggregates multiple ConfigSources into one and add the defaults (FileConfigSource)
    */
  def aggregate[F[_]: Async: Files](
      extraSources: List[ConfigSource[F]]
  )(implicit log: Logger): F[ConfigSource[F]] = {

    FileConfigSource.load[F]().flatMap { fileConfigSource =>
      val defaultSources = NonEmptyList.of(
        fileConfigSource
      )

      val allSources = (defaultSources ++ extraSources).sortBy(_.priority)
      Async[F]
        .delay(log.debug(s"Loaded config sources ${allSources.map(_.name).mkString_(" | ")}"))
        .as(new AggregateConfigSource[F](allSources))
    }
  }

  implicit final class ConfigSourceOps[F[_]](val source: ConfigSource[F]) extends AnyVal {
    def withDefaults: ConfigSource[F] = new DefaultsConfigSource[F](source)
  }
}
