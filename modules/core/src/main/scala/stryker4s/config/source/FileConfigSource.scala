package stryker4s.config.source

import cats.effect.{Async, Sync}
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

class FileConfigSource[F[_]](h: ConfigValue[F, Hocon.HoconAt])
    extends ConfigSource[F]
    with CirisConfigDecoders
    with HoconConfigDecoders {

  override def name: String = "file config"

  def readHocon(name: String) = h.flatMap(_(name))

  override def priority: ConfigOrder = ConfigOrder(15)

  override def testFilter: ConfigValue[F, Seq[String]] = readHocon("test-filter").as[Seq[String]]
  override def mutate: ConfigValue[F, Seq[String]] = readHocon("mutate").as[Seq[String]]

  override def baseDir: ConfigValue[F, Path] = readHocon("base-dir").as[Path]

  override def reporters: ConfigValue[F, Seq[ReporterType]] = readHocon("reporters").as[Seq[ReporterType]]

  override def files: ConfigValue[F, Seq[String]] = readHocon("files").as[Seq[String]]

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] =
    readHocon("excluded-mutations").as[Seq[String]].as[Seq[ExcludedMutation]]

  override def thresholdsHigh: ConfigValue[F, Int] = readHocon("thresholds.high").as[Int]
  override def thresholdsLow: ConfigValue[F, Int] = readHocon("thresholds.low").as[Int]
  override def thresholdsBreak: ConfigValue[F, Int] = readHocon("thresholds.break").as[Int]

  override def dashboardBaseUrl: ConfigValue[F, Uri] =
    readHocon("dashboard.base-url").as[Uri]
  override def dashboardReportType: ConfigValue[F, DashboardReportType] =
    readHocon("dashboard.report-type").as[DashboardReportType]
  override def dashboardProject: ConfigValue[F, Option[String]] =
    readHocon("dashboard.project").as[String].map(_.some)
  override def dashboardVersion: ConfigValue[F, Option[String]] =
    readHocon("dashboard.version").as[String].map(_.some)
  override def dashboardModule: ConfigValue[F, Option[String]] =
    readHocon("dashboard.module").as[String].map(_.some)

  override def timeout: ConfigValue[F, FiniteDuration] = readHocon("timeout").as[FiniteDuration]

  override def timeoutFactor: ConfigValue[F, Double] = readHocon("timeout-factor").as[Double]

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] =
    readHocon("max-test-runner-reuse").as[Int].map(_.some)

  override def legacyTestRunner: ConfigValue[F, Boolean] = readHocon("legacy-test-runner").as[Boolean]

  override def scalaDialect: ConfigValue[F, Dialect] = readHocon("scala-dialect").as[Dialect]

  override def concurrency: ConfigValue[F, Int] = readHocon("concurrency").as[Int]

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] =
    readHocon("debug.log-test-runner-stdout").as[Boolean]
  override def debugDebugTestRunner: ConfigValue[F, Boolean] = readHocon("debug.debug-test-runner").as[Boolean]

  override def staticTmpDir: ConfigValue[F, Boolean] = readHocon("static-tmp-dir").as[Boolean]

  override def cleanTmpDir: ConfigValue[F, Boolean] = readHocon("clean-tmp-dir").as[Boolean]

  override def testRunnerCommand: ConfigValue[F, String] = readHocon("test-runner.command").as[String]
  override def testRunnerArgs: ConfigValue[F, String] = readHocon("test-runner.args").as[String]

}

object FileConfigSource {
  def load[F[_]: Async: Files]()(implicit log: Logger) = {
    // Read the config file, and parse it as HOCON if it exists. Otherwise this ConfigSource resolves missing (or failed) for all values.
    // TODO: configurable config file path
    val configPath = Path("stryker4s.conf")
    Sync[F].delay(log.debug(s"Attempting to read config from ${Underlined.On(configPath.toString)}")) *>
      Files[F]
        .readUtf8(configPath)
        .compile
        .string
        .attempt
        .flatMap(_.flatTraverse { content =>
          Sync[F].delay {
            val config = ConfigFactory.parseString(content)
            Hocon.hoconAt(config)("stryker4s", configPath)
          }.attempt
        })
        .flatMap[ConfigValue[Effect, Hocon.HoconAt]] {
          case Right(hocon) => ConfigValue.default(hocon).pure[F]
          case Left(_: NoSuchFileException) =>
            Sync[F].delay(log.debug("")) *> ConfigValue.missing(configPath.toString).pure[F]
          case Left(value) =>
            ConfigValue.failed(ConfigError(s"error reading $configPath: '${value.getMessage()}'")).pure[F]
        }
        .map(new FileConfigSource[F](_))
  }
}
