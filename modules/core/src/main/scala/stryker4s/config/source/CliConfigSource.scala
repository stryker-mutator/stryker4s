package stryker4s.config.source

import cats.syntax.all.*
import ciris.*
import fs2.io.file.Path
import stryker4s.config.codec.CirisConfigDecoders
import stryker4s.config.{ConfigOrder, DashboardReportType, ExcludedMutation, ReporterType}
import sttp.model.Uri

import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect
import com.monovore.decline.Command
import com.monovore.decline.Opts

class CliConfigSource[F[_]](args: Seq[String]) extends ConfigSource[F] with CirisConfigDecoders {

  def strykerCommand[T]: Opts[T] => Command[T] = Command[T](
    name = "stryker",
    header = "Stryker4s - A mutation testing tool for Scala"
  )

  def parseOpt[T](opt: Opts[T]): ConfigValue[F, T] = {
    strykerCommand(opt).parse(args) match {
      case Right(value) => ConfigValue.loaded(ConfigKey(implicitly[sourcecode.Name].value), value)
      case Left(_)      => ConfigValue.missing(implicitly[sourcecode.Name].value)
    }
  }
  override def name: String = "CLI arguments"

  override def priority: ConfigOrder = ConfigOrder(5)

  private def splitArg(opts: Opts[String]): ConfigValue[F, Seq[String]] = parseOpt[String](opts).map(_.split(",").toSeq)

  override def testFilter: ConfigValue[F, Seq[String]] = splitArg(
    Opts.option[String]("test-filter", short = "t", help = "A regex to select which tests to run.")
  )

  override def mutate: ConfigValue[F, Seq[String]] = splitArg(
    Opts.option[String]("mutate", short = "m", help = "A regex to select which mutations to apply.")
  )

  override def baseDir: ConfigValue[F, Path] =
    parseOpt[String](Opts.option[String]("base-dir", short = "b", help = "The root of the project.")).as[Path]

  override def reporters: ConfigValue[F, Seq[ReporterType]] = splitArg(
    Opts.option[String]("reporters", short = "r", help = "The reporters to use.")
  ).as[Seq[ReporterType]]

  override def files: ConfigValue[F, Seq[String]] = splitArg(
    Opts.option[String]("files", short = "f", help = "The files to mutate.")
  )

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] = splitArg(
    Opts.option[String]("excluded-mutations", short = "e", help = "The mutations to exclude.")
  ).as[Seq[ExcludedMutation]]

  override def thresholdsHigh: ConfigValue[F, Int] = parseOpt[Int](
    Opts.option[Int]("thresholds.high", short = "h", help = "The high threshold.")
  )

  override def thresholdsLow: ConfigValue[F, Int] = parseOpt[Int](
    Opts.option[Int]("thresholds.low", short = "l", help = "The low threshold.")
  )

  override def thresholdsBreak: ConfigValue[F, Int] = parseOpt[Int](
    Opts.option[Int]("thresholds.break", short = "k", help = "The break threshold.")
  )

  override def dashboardBaseUrl: ConfigValue[F, Uri] = parseOpt[String](
    Opts.option[String]("dashboard.base-url", short = "u", help = "The base url of the dashboard.")
  ).as[Uri]

  override def dashboardReportType: ConfigValue[F, DashboardReportType] = parseOpt[String](
    Opts.option[String]("dashboard.report-type", short = "d", help = "The report type of the dashboard.")
  ).as[DashboardReportType]

  override def dashboardProject: ConfigValue[F, Option[String]] = parseOpt[String](
    Opts.option[String]("dashboard.project", short = "p", help = "The project name of the dashboard.")
  ).map(_.some)

  override def dashboardVersion: ConfigValue[F, Option[String]] = parseOpt[String](
    Opts.option[String]("dashboard.version", short = "v", help = "The version of the dashboard.")
  ).map(_.some)

  override def dashboardModule: ConfigValue[F, Option[String]] = parseOpt[String](
    Opts.option[String]("dashboard.module", short = "o", help = "The module of the dashboard.")
  ).map(_.some)

  override def timeout: ConfigValue[F, FiniteDuration] = parseOpt[FiniteDuration](
    Opts.option[FiniteDuration]("timeout", short = "t", help = "The timeout for a test run.")
  )

  override def timeoutFactor: ConfigValue[F, Double] = parseOpt[Double](
    Opts.option[Double]("timeout-factor", short = "f", help = "The factor to multiply the timeout with.")
  )

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = parseOpt[Int](
    Opts.option[Int](
      "max-test-runner-reuse",
      short = "r",
      help = "The maximum amount of times a test runner can be reused."
    )
  ).map(_.some)

  override def legacyTestRunner: ConfigValue[F, Boolean] = parseOpt[Boolean](
    Opts.flag("legacy-test-runner", short = "l", help = "Use the legacy test runner.").orFalse
  )

  override def scalaDialect: ConfigValue[F, Dialect] = parseOpt[String](
    Opts.option[String]("scala-dialect", short = "s", help = "The scala dialect to use.")
  ).as[Dialect]

  override def concurrency: ConfigValue[F, Int] = parseOpt[Int](
    Opts.option[Int]("concurrency", short = "c", help = "The amount of concurrent test runners.")
  )

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] = parseOpt[Boolean](
    Opts.flag("debug.log-test-runner-stdout", short = "d", help = "Log the test runner stdout.").orFalse
  )
  override def debugDebugTestRunner: ConfigValue[F, Boolean] = parseOpt[Boolean](
    Opts.flag("debug.debug-test-runner", short = "d", help = "Debug the test runner.").orFalse
  )

  override def staticTmpDir: ConfigValue[F, Boolean] = parseOpt[Boolean](
    Opts.flag("static-tmp-dir", short = "s", help = "Use a static tmp directory.").orFalse
  )

  override def cleanTmpDir: ConfigValue[F, Boolean] = parseOpt[Boolean](
    Opts.flag("clean-tmp-dir", short = "c", help = "Clean the tmp directory.").orFalse
  )

  override def testRunnerCommand: ConfigValue[F, String] = parseOpt[String](
    Opts.option[String]("test-runner.command", short = "c", help = "The test runner command.")
  )

  override def testRunnerArgs: ConfigValue[F, String] = parseOpt(
    Opts.option[String]("test-runner.args", short = "a", help = "The test runner arguments.")
  )

  override def openReport: ConfigValue[F, Boolean] = parseOpt[Boolean](
    Opts.flag("open-report", short = "o", help = "Opens the report in the default browser.").orFalse
  )
}
