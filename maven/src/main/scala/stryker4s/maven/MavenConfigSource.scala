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

// TODO: Add missing implementations
class MavenConfigSource[F[_]](project: MavenProject) extends ConfigSource[F] {

  override def name: String = "maven"

  override def priority: ConfigOrder = ConfigOrder(15)

  def mvnKey(key: String): ConfigKey = ConfigKey(s"$name $key")

  override def mutate: ConfigValue[F, Seq[String]] = notSupported("mutate")

  override def testFilter: ConfigValue[F, Seq[String]] = notSupported("testFilter")

  override def baseDir: ConfigValue[F, Path] = notSupported("baseDir")

  override def reporters: ConfigValue[F, Seq[ReporterType]] = notSupported("reporters")

  override def files: ConfigValue[F, Seq[String]] = {
    val sources = project.getCompileSourceRoots().asScala.map(_ + "/**/*.scala").toSeq
    ConfigValue.loaded(mvnKey("files"), sources)
  }

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] =
    notSupported("excludedMutations")

  override def thresholdsHigh: ConfigValue[F, Int] = notSupported("thresholdsHigh")

  override def thresholdsLow: ConfigValue[F, Int] = notSupported("thresholdsLow")

  override def thresholdsBreak: ConfigValue[F, Int] = notSupported("thresholdsBreak")

  override def dashboardBaseUrl: ConfigValue[F, Uri] = notSupported("dashboardBaseUrl")

  override def dashboardReportType: ConfigValue[F, DashboardReportType] =
    notSupported("dashboardReportType")

  override def dashboardProject: ConfigValue[F, Option[String]] = notSupported("dashboardProject")

  override def dashboardVersion: ConfigValue[F, Option[String]] = notSupported("dashboardVersion")

  override def dashboardModule: ConfigValue[F, Option[String]] = notSupported("dashboardModule")

  override def timeout: ConfigValue[F, FiniteDuration] = notSupported("timeout")

  override def timeoutFactor: ConfigValue[F, Double] = notSupported("timeoutFactor")

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = notSupported("maxTestRunnerReuse")

  override def legacyTestRunner: ConfigValue[F, Boolean] = notSupported("legacyTestRunner")

  override def scalaDialect: ConfigValue[F, Dialect] = notSupported("scalaDialect")

  override def concurrency: ConfigValue[F, Int] = notSupported("concurrency")

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] =
    notSupported("debugLogTestRunnerStdout")

  override def debugDebugTestRunner: ConfigValue[F, Boolean] = notSupported("debugDebugTestRunner")

  override def staticTmpDir: ConfigValue[F, Boolean] = notSupported("staticTmpDir")

  override def cleanTmpDir: ConfigValue[F, Boolean] = notSupported("cleanTmpDir")

  override def testRunnerCommand: ConfigValue[F, String] = notSupported("testRunnerCommand")

  override def testRunnerArgs: ConfigValue[F, String] = notSupported("testRunnerArgs")

}

// override def files: fs2.Stream[IO, Path] =
//   Stream
//     .emits(project.getCompileSourceRoots().asScala)
//     .map(Path(_))
//     .evalFilter(Files[IO].exists)
//     .flatMap(Glob.glob(_, Seq("**/*.scala")))
