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

import java.nio.charset.Charset
import java.nio.file.NoSuchFileException
import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect

class FileConfigSource[F[_]](h: ConfigValue[F, Hocon.HoconAt])
    extends ConfigSource[F]
    with CirisConfigDecoders
    with HoconConfigDecoders {

  override def name: String = "file config"

  def read(name: String) = h.flatMap(_(name))

  override def priority: ConfigOrder = ConfigOrder(15)

  override def testFilter: ConfigValue[F, Seq[String]] = read("test-filter").as[Seq[String]]
  override def mutate: ConfigValue[F, Seq[String]] = read("mutate").as[Seq[String]]

  override def baseDir: ConfigValue[F, Path] = read("base-dir").as[Path]

  override def reporters: ConfigValue[F, Seq[ReporterType]] = read("reporters").as[Seq[ReporterType]]

  override def files: ConfigValue[F, Seq[String]] = read("files").as[Seq[String]]

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] =
    read("excluded-mutations").as[Seq[ExcludedMutation]]

  override def thresholdsHigh: ConfigValue[F, Int] = read("thresholds.high").as[Int]
  override def thresholdsLow: ConfigValue[F, Int] = read("thresholds.low").as[Int]
  override def thresholdsBreak: ConfigValue[F, Int] = read("thresholds.break").as[Int]

  override def dashboardBaseUrl: ConfigValue[F, Uri] =
    read("dashboard.base-url").as[Uri]
  override def dashboardReportType: ConfigValue[F, DashboardReportType] =
    read("dashboard.report-type").as[DashboardReportType]
  override def dashboardProject: ConfigValue[F, Option[String]] =
    read("dashboard.project").as[String].map(_.some)
  override def dashboardVersion: ConfigValue[F, Option[String]] =
    read("dashboard.version").as[String].map(_.some)
  override def dashboardModule: ConfigValue[F, Option[String]] =
    read("dashboard.module").as[String].map(_.some)

  override def timeout: ConfigValue[F, FiniteDuration] = read("timeout").as[FiniteDuration]

  override def timeoutFactor: ConfigValue[F, Double] = read("timeout-factor").as[Double]

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] =
    read("max-test-runner-reuse").as[Int].map(_.some)

  override def legacyTestRunner: ConfigValue[F, Boolean] = read("legacy-test-runner").as[Boolean]

  override def scalaDialect: ConfigValue[F, Dialect] = read("scala-dialect").as[Dialect]

  override def concurrency: ConfigValue[F, Int] = read("concurrency").as[Int]

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] =
    read("debug.log-test-runner-stdout").as[Boolean]
  override def debugDebugTestRunner: ConfigValue[F, Boolean] = read("debug.debug-test-runner").as[Boolean]

  override def staticTmpDir: ConfigValue[F, Boolean] = read("static-tmp-dir").as[Boolean]

  override def cleanTmpDir: ConfigValue[F, Boolean] = read("clean-tmp-dir").as[Boolean]

  override def testRunnerCommand: ConfigValue[F, String] = read("test-runner.command").as[String]
  override def testRunnerArgs: ConfigValue[F, String] = read("test-runner.args").as[String]

  override def openReportAutomatically: ConfigValue[F, Boolean] = read("open-report-automatically").as[Boolean]

}

object FileConfigSource {
  // TODO: configurable config file path
  def load[F[_]: Async: Files](configPath: Path = Path("stryker4s.conf"))(implicit log: Logger) = {
    // Read the config file, and parse it as HOCON if it exists. Otherwise this ConfigSource resolves missing (or failed) for all values.
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
        .map[ConfigValue[Effect, Hocon.HoconAt]] {
          case Right(hocon) =>
            // This has to be 'default' and not 'loaded' to still allow other config sources to override it
            ConfigValue.default(hocon)
          case Left(_: NoSuchFileException) =>
            ConfigValue.missing(ConfigKey.file(configPath.toNioPath, Charset.forName("UTF-8")))
          case Left(value) =>
            ConfigValue.failed(ConfigError(s"error reading $configPath: '${value.getMessage()}'"))
        }
        .map(new FileConfigSource[F](_))
  }
}
