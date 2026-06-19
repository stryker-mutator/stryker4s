package stryker4s.maven

import cats.syntax.all.*
import ciris.{ConfigKey, ConfigValue}
import fs2.io.file.Path
import org.apache.maven.project.MavenProject
import stryker4s.config.codec.CirisConfigDecoders
import stryker4s.config.source.ConfigSource
import stryker4s.config.{ConfigOrder, DashboardReportType, ExcludedMutation, ReporterType}
import stryker4s.maven.runner.ScalaVersions
import sttp.model.Uri

import java.net.URI
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*
import scala.meta.{dialects, Dialect}
import scala.util.Try

class MavenConfigSource[F[_]](project: MavenProject) extends ConfigSource[F] with CirisConfigDecoders {

  override def name: String = "maven"

  override def priority: ConfigOrder = ConfigOrder(15)

  override def mutate: ConfigValue[F, Seq[String]] = ConfigValue.loaded(
    ConfigKey(summon[sourcecode.Name].value),
    project.getCompileSourceRoots().asScala.map(_ + "/**.scala").toSeq
  )

  override def testFilter: ConfigValue[F, Seq[String]] = notSupported

  override def baseDir: ConfigValue[F, Path] = ConfigValue.loaded(
    ConfigKey(summon[sourcecode.Name].value),
    Path.fromNioPath(project.getBasedir().toPath())
  )

  override def reporters: ConfigValue[F, Seq[ReporterType]] = notSupported

  override def files: ConfigValue[F, Seq[String]] = ConfigValue.loaded(
    ConfigKey(summon[sourcecode.Name].value),
    (project.getCompileSourceRoots().asScala ++ project.getTestCompileSourceRoots().asScala).map(_ + "/**").toSeq
  )

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] =
    notSupported

  override def thresholdsHigh: ConfigValue[F, Int] = notSupported

  override def thresholdsLow: ConfigValue[F, Int] = notSupported

  override def thresholdsBreak: ConfigValue[F, Int] = notSupported

  override def dashboardBaseUrl: ConfigValue[F, Uri] = notSupported

  override def dashboardReportType: ConfigValue[F, DashboardReportType] =
    notSupported

  // Derived from the project's SCM url (e.g. `github.com/org/repo`), as the sbt plugin does from `scmInfo`
  override def dashboardProject: ConfigValue[F, Option[String]] = dashboardProjectFromScm match {
    case Some(found) => ConfigValue.loaded(ConfigKey(summon[sourcecode.Name].value), found.some)
    case None        => notSupported
  }

  override def dashboardVersion: ConfigValue[F, Option[String]] = notSupported

  override def dashboardModule: ConfigValue[F, Option[String]] = ConfigValue.loaded(
    ConfigKey(summon[sourcecode.Name].value),
    Option(project.getArtifactId())
  )

  override def timeout: ConfigValue[F, FiniteDuration] = notSupported

  override def timeoutFactor: ConfigValue[F, Double] = notSupported

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = notSupported

  override def legacyTestRunner: ConfigValue[F, Boolean] = notSupported

  // Derived from the project's Scala version (and `-Xsource:3`), so Scala 3 sources are parsed with a Scala 3 dialect
  override def scalaDialect: ConfigValue[F, Dialect] = ScalaVersions.fullVersion(project) match {
    case Some(version) => ConfigValue.loaded(ConfigKey(summon[sourcecode.Name].value), derivedDialect(version))
    case None          => notSupported
  }

  override def concurrency: ConfigValue[F, Int] = notSupported

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] = notSupported

  override def debugDebugTestRunner: ConfigValue[F, Boolean] = notSupported

  override def staticTmpDir: ConfigValue[F, Boolean] = notSupported

  override def cleanTmpDir: ConfigValue[F, Boolean] = notSupported

  override def testRunnerCommand: ConfigValue[F, String] = notSupported

  override def testRunnerArgs: ConfigValue[F, String] = notSupported

  override def openReport: ConfigValue[F, Boolean] = notSupported

  override def showHelpMessage: ConfigValue[F, Option[String]] = notSupported

  /** The scalameta dialect for the project's Scala version, mirroring the sbt and Mill plugins. */
  private def derivedDialect(scalaVersion: String): Dialect = {
    val hasSource3 = ScalaVersions.scalacOptions(project).exists(_.startsWith("-Xsource:3"))
    def reader(major: String, minor: String, source3: Boolean): Option[Dialect] =
      dialectReader.decode(None, s"scala$major$minor${if source3 then "source3" else ""}").toOption

    scalaVersion.split('.').toList match {
      case "3" :: minor :: _   => reader("3", minor, source3 = false).getOrElse(dialects.Scala3)
      case major :: minor :: _ =>
        reader(major, minor, hasSource3).getOrElse(if hasSource3 then dialects.Scala213Source3 else dialects.Scala213)
      case _ => if hasSource3 then dialects.Scala213Source3 else dialects.Scala213
    }
  }

  /** Parse the project's SCM url into the dashboard's `gitProvider/organization/repository` format. */
  private def dashboardProjectFromScm: Option[String] =
    Option(project.getScm())
      .flatMap(scm => Option(scm.getUrl()).orElse(Option(scm.getConnection())))
      .flatMap { scmUrl =>
        // Strip an `scm:<provider>:` prefix and any `.git` suffix, then read host + path
        val cleaned = scmUrl.replaceFirst("^scm:[a-z]+:", "").stripSuffix(".git")
        Try(new URI(cleaned)).toOption.flatMap { uri =>
          for {
            host <- Option(uri.getHost)
            if host.startsWith("github.com")
            path = Option(uri.getPath).getOrElse("").stripPrefix("/").stripSuffix("/")
            if path.nonEmpty
          } yield s"$host/$path"
        }
      }

}
