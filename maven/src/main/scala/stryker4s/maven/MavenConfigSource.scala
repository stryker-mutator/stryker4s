package stryker4s.maven

import ciris.{ConfigKey, ConfigValue}
import fs2.io.file.Path
import org.apache.maven.project.MavenProject
import stryker4s.config.source.ConfigSource
import stryker4s.config.{ConfigOrder, DashboardReportType, ExcludedMutation, ReporterType}
import sttp.model.Uri

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*
import scala.meta.Dialect

// TODO: Add missing implementations and resolving config from maven pom.xml
class MavenConfigSource[F[_]](project: MavenProject) extends ConfigSource[F] {

  override def name: String = "maven"

  override def priority: ConfigOrder = ConfigOrder(15)

  override def mutate: ConfigValue[F, Seq[String]] = ConfigValue.loaded(
    ConfigKey(implicitly[sourcecode.Name].value),
    project.getCompileSourceRoots().asScala.map(_ + "/**.scala").toSeq
  )

  override def testFilter: ConfigValue[F, Seq[String]] = notSupported

  override def baseDir: ConfigValue[F, Path] = notSupported

  override def reporters: ConfigValue[F, Seq[ReporterType]] = notSupported

  override def files: ConfigValue[F, Seq[String]] = notSupported

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] =
    notSupported

  override def thresholdsHigh: ConfigValue[F, Int] = notSupported

  override def thresholdsLow: ConfigValue[F, Int] = notSupported

  override def thresholdsBreak: ConfigValue[F, Int] = notSupported

  override def dashboardBaseUrl: ConfigValue[F, Uri] = notSupported

  override def dashboardReportType: ConfigValue[F, DashboardReportType] =
    notSupported

  override def dashboardProject: ConfigValue[F, Option[String]] = notSupported

  override def dashboardVersion: ConfigValue[F, Option[String]] = notSupported

  override def dashboardModule: ConfigValue[F, Option[String]] = notSupported

  override def timeout: ConfigValue[F, FiniteDuration] = notSupported

  override def timeoutFactor: ConfigValue[F, Double] = notSupported

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = notSupported

  override def legacyTestRunner: ConfigValue[F, Boolean] = notSupported

  override def scalaDialect: ConfigValue[F, Dialect] = notSupported

  override def concurrency: ConfigValue[F, Int] = notSupported

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] = notSupported

  override def debugDebugTestRunner: ConfigValue[F, Boolean] = notSupported

  override def staticTmpDir: ConfigValue[F, Boolean] = notSupported

  override def cleanTmpDir: ConfigValue[F, Boolean] = notSupported

  override def testRunnerCommand: ConfigValue[F, String] = notSupported

  override def testRunnerArgs: ConfigValue[F, String] = notSupported

  override def openReport: ConfigValue[F, Boolean] = notSupported

  override def showHelpMessage: ConfigValue[F, Option[String]] = notSupported

}
