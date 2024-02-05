package stryker4s.config.source

import cats.effect.Async
import cats.syntax.all.*
import ciris.*
import com.typesafe.config.ConfigFactory
import fansi.Underlined
import fs2.io.file.{Files, Path}
import stryker4s.config.codec.{CirisConfigDecoders, Hocon, HoconConfigDecoders}
import stryker4s.config.{ConfigOrder, DashboardReportType, ExcludedMutation, ReporterType}
import stryker4s.log.Logger
import sttp.model.Uri

import java.nio.file.NoSuchFileException
import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect
import scala.util.Try

class FileConfigSource[F[_]](h: ConfigValue[F, Hocon.HoconAt])
    extends ConfigSource[F]
    with CirisConfigDecoders
    with HoconConfigDecoders {

  override def name: String = "file config"

  def findInConfig(name: String) = h.flatMap(_.apply(name))

  override def priority: ConfigOrder = ConfigOrder(15)

  override def testFilter: ConfigValue[F, Seq[String]] = findInConfig("test-filter").as[Seq[String]]
  override def mutate: ConfigValue[F, Seq[String]] = findInConfig("mutate").as[Seq[String]]

  override def baseDir: ConfigValue[F, Path] = findInConfig("base-dir").as[Path]

  override def reporters: ConfigValue[F, Seq[ReporterType]] = findInConfig("reporters").as[Seq[ReporterType]]

  override def files: ConfigValue[F, Seq[String]] = findInConfig("files").as[Seq[String]]

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] =
    findInConfig("excluded-mutations").as[Seq[String]].as[Seq[ExcludedMutation]]

  override def thresholdsHigh: ConfigValue[F, Int] = findInConfig("thresholds.high").as[Int]
  override def thresholdsLow: ConfigValue[F, Int] = findInConfig("thresholds.low").as[Int]
  override def thresholdsBreak: ConfigValue[F, Int] = findInConfig("thresholds.break").as[Int]

  override def dashboardBaseUrl: ConfigValue[F, Uri] =
    findInConfig("dashboard.base-url").as[Uri]
  override def dashboardReportType: ConfigValue[F, DashboardReportType] =
    findInConfig("dashboard.report-type").as[DashboardReportType]
  override def dashboardProject: ConfigValue[F, Option[String]] =
    findInConfig("dashboard.project").as[String].map(_.some)
  override def dashboardVersion: ConfigValue[F, Option[String]] =
    findInConfig("dashboard.version").as[String].map(_.some)
  override def dashboardModule: ConfigValue[F, Option[String]] =
    findInConfig("dashboard.module").as[String].map(_.some)

  override def timeout: ConfigValue[F, FiniteDuration] = findInConfig("timeout").as[FiniteDuration]

  override def timeoutFactor: ConfigValue[F, Double] = findInConfig("timeout-factor").as[Double]

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] =
    findInConfig("max-test-runner-reuse").as[Int].map(_.some)

  override def legacyTestRunner: ConfigValue[F, Boolean] = findInConfig("legacy-test-runner").as[Boolean]

  override def scalaDialect: ConfigValue[F, Dialect] = findInConfig("scala-dialect").as[Dialect]

  override def concurrency: ConfigValue[F, Int] = findInConfig("concurrency").as[Int]

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] =
    findInConfig("debug.log-test-runner-stdout").as[Boolean]
  override def debugDebugTestRunner: ConfigValue[F, Boolean] = findInConfig("debug.debug-test-runner").as[Boolean]

  override def staticTmpDir: ConfigValue[F, Boolean] = findInConfig("static-tmp-dir").as[Boolean]

  override def cleanTmpDir: ConfigValue[F, Boolean] = findInConfig("clean-tmp-dir").as[Boolean]

}

object FileConfigSource {
  def load[F[_]: Async: Files]()(implicit log: Logger) = {
    // Read the config file, and parse it as HOCON if it exists. Otherwise this ConfigSource resolves missing (or failed) for all values.
    // TODO: configurable config file path
    val configPath = Path("stryker4s.conf")
    Async[F].delay(log.debug(s"Attempting to read config from ${Underlined.On(configPath.toString)}")) *>
      Files[F]
        .readUtf8(configPath)
        .compile
        .string
        .attempt
        .map[ConfigValue[F, Hocon.HoconAt]] {
          _.flatMap { content =>
            Try {
              val config = ConfigFactory.parseString(content)
              Hocon.hoconAt(config)("stryker4s", configPath)
            }.toEither
          } match {
            case Right(hocon)                 => ConfigValue.default(hocon)
            case Left(_: NoSuchFileException) => ConfigValue.missing(configPath.toString)
            case Left(value) => ConfigValue.failed(ConfigError(s"error reading $configPath: '${value.getMessage()}'"))
          }
        }
        .map(new FileConfigSource[F](_))
  }
}
