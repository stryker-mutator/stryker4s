package stryker4s.config.source

import cats.syntax.option.*
import ciris.ConfigValue
import fs2.io.file.Path
import stryker4s.config.*
import sttp.client3.UriContext
import sttp.model.Uri

import scala.concurrent.duration.*
import scala.meta.{dialects, Dialect}

/** Default values for config values
  */
class DefaultsConfigSource[F[_]]() extends ConfigSource[F] {

  override def name: String = "defaults"

  override def priority: ConfigOrder = ConfigOrder.Last

  override def mutate: ConfigValue[F, Seq[String]] = ConfigValue.default(Seq("**/main/scala/**.scala"))

  override def testFilter: ConfigValue[F, Seq[String]] = ConfigValue.default(Seq.empty)

  override def baseDir: ConfigValue[F, Path] = ConfigValue.default(Path("").absolute)

  override def reporters: ConfigValue[F, Seq[ReporterType]] = ConfigValue.default(Seq(Console, Html))

  override def files: ConfigValue[F, Seq[String]] = ConfigValue.default(
    Seq(
      "**",
      "!target/**",
      "!project/**",
      "!.metals/**",
      "!.bloop/**",
      "!.idea/**"
    )
  )

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] = ConfigValue.default(Seq.empty)

  override def thresholdsHigh: ConfigValue[F, Int] = ConfigValue.default(80)

  override def thresholdsLow: ConfigValue[F, Int] = ConfigValue.default(60)

  override def thresholdsBreak: ConfigValue[F, Int] = ConfigValue.default(0)

  override def dashboardBaseUrl: ConfigValue[F, Uri] =
    ConfigValue.default(uri"https://dashboard.stryker-mutator.io")

  override def dashboardReportType: ConfigValue[F, DashboardReportType] = ConfigValue.default(Full)

  override def dashboardProject: ConfigValue[F, Option[String]] = ConfigValue.default(none)

  override def dashboardVersion: ConfigValue[F, Option[String]] = ConfigValue.default(none)

  override def dashboardModule: ConfigValue[F, Option[String]] = ConfigValue.default(none)

  override def timeout: ConfigValue[F, FiniteDuration] = ConfigValue.default(5.seconds)

  override def timeoutFactor: ConfigValue[F, Double] = ConfigValue.default(1.5)

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = ConfigValue.default(none)

  override def legacyTestRunner: ConfigValue[F, Boolean] = ConfigValue.default(false)

  override def scalaDialect: ConfigValue[F, Dialect] = ConfigValue.default(dialects.Scala213Source3)

  override def concurrency: ConfigValue[F, Int] = ConfigValue.default(Config.defaultConcurrency)

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] = ConfigValue.default(false)

  override def debugDebugTestRunner: ConfigValue[F, Boolean] = ConfigValue.default(false)

  override def staticTmpDir: ConfigValue[F, Boolean] = ConfigValue.default(false)

  override def cleanTmpDir: ConfigValue[F, Boolean] = ConfigValue.default(true)

  override def testRunnerCommand: ConfigValue[F, String] = ConfigValue.default("sbt")
  override def testRunnerArgs: ConfigValue[F, String] = ConfigValue.default("test")
  override def openReport: ConfigValue[F, Boolean] = ConfigValue.default(false)
}
