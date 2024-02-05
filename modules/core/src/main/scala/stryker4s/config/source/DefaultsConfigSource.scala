package stryker4s.config.source

import cats.syntax.option.*
import ciris.ConfigValue
import fs2.io.file.Path
import stryker4s.config.*
import sttp.client3.UriContext
import sttp.model.Uri

import scala.concurrent.duration.*
import scala.meta.{dialects, Dialect}

/** Wraps a ConfigSource and sets default values for some of the config values
  */
class DefaultsConfigSource[F[_]](inner: ConfigSource[F]) extends ConfigSource[F] {

  override def name: String = "defaults"

  override def priority: ConfigOrder = inner.priority

  override def mutate: ConfigValue[F, Seq[String]] = inner.mutate.default(Seq("**/main/scala/**.scala"))

  override def testFilter: ConfigValue[F, Seq[String]] = inner.testFilter.default(Seq.empty)

  override def baseDir: ConfigValue[F, Path] = inner.baseDir.default(Path(".").absolute)

  override def reporters: ConfigValue[F, Seq[ReporterType]] = inner.reporters.default(Seq(Console, Html))

  override def files: ConfigValue[F, Seq[String]] = inner.files.default(Seq("**/main/scala/**/*.scala"))

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] = inner.excludedMutations.default(Seq.empty)

  override def thresholdsHigh: ConfigValue[F, Int] = inner.thresholdsHigh.default(80)

  override def thresholdsLow: ConfigValue[F, Int] = inner.thresholdsLow.default(60)

  override def thresholdsBreak: ConfigValue[F, Int] = inner.thresholdsBreak.default(0)

  override def dashboardBaseUrl: ConfigValue[F, Uri] =
    inner.dashboardBaseUrl.default(uri"https://dashboard.stryker-mutator.io")

  override def dashboardReportType: ConfigValue[F, DashboardReportType] = inner.dashboardReportType.default(Full)

  override def dashboardProject: ConfigValue[F, Option[String]] = inner.dashboardProject.default(none)

  override def dashboardVersion: ConfigValue[F, Option[String]] = inner.dashboardVersion.default(none)

  override def dashboardModule: ConfigValue[F, Option[String]] = inner.dashboardModule.default(none)

  override def timeout: ConfigValue[F, FiniteDuration] = inner.timeout.default(5.seconds)

  override def timeoutFactor: ConfigValue[F, Double] = inner.timeoutFactor.default(1.5)

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = inner.maxTestRunnerReuse.default(none)

  override def legacyTestRunner: ConfigValue[F, Boolean] = inner.legacyTestRunner.default(false)

  override def scalaDialect: ConfigValue[F, Dialect] = inner.scalaDialect.default(dialects.Scala213Source3)

  override def concurrency: ConfigValue[F, Int] = inner.concurrency.default(Config.defaultConcurrency)

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] = inner.debugLogTestRunnerStdout.default(false)

  override def debugDebugTestRunner: ConfigValue[F, Boolean] = inner.debugDebugTestRunner.default(false)

  override def staticTmpDir: ConfigValue[F, Boolean] = inner.staticTmpDir.default(false)

  override def cleanTmpDir: ConfigValue[F, Boolean] = inner.cleanTmpDir.default(true)

}
