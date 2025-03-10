package stryker4s.config.source

import cats.syntax.all.*
import ciris.*
import com.monovore.decline.{Command, Opts}
import fs2.io.file.Path
import stryker4s.config.codec.CirisConfigDecoders
import stryker4s.config.{ConfigOrder, DashboardReportType, ExcludedMutation, ReporterType}
import sttp.model.Uri

import java.nio.file.Path as JPath
import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect

class CliConfigSource[F[_]](args: Seq[String]) extends ConfigSource[F] with CirisConfigDecoders {

  private def parseOpt[T](opt: Opts[T])(implicit name: sourcecode.Name): ConfigValue[F, T] =
    opts.strykerCommand(opt).parse(args) match {
      case Right(value) => ConfigValue.loaded(ConfigKey(name.value), value)
      case Left(help) if help.errors.nonEmpty =>
        val missing = "Missing "
        // ConfigValue.missing also adds a "Missing " prefix, so we need to remove it
        help.errors
          .map(e => ConfigValue.missing[T](e.replaceFirst(missing, "")))
          .reduce(_ or _)
      case Left(help) =>
        ConfigValue.failed(ConfigError(help.usage.mkString("\n")))
    }

  override def name: String = "CLI arguments"

  override def priority: ConfigOrder = ConfigOrder(5)

  override def mutate: ConfigValue[F, Seq[String]] = parseOpt(opts.mutate)

  override def testFilter: ConfigValue[F, Seq[String]] = parseOpt(opts.testFilter)

  override def baseDir: ConfigValue[F, Path] = parseOpt(opts.baseDir)

  override def reporters: ConfigValue[F, Seq[ReporterType]] = parseOpt(opts.reporters).as[Seq[ReporterType]]

  override def files: ConfigValue[F, Seq[String]] = parseOpt(opts.files)

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] =
    parseOpt(opts.excludedMutations).as[Seq[ExcludedMutation]]

  override def thresholdsHigh: ConfigValue[F, Int] = parseOpt(opts.thresholdsHigh)
  override def thresholdsLow: ConfigValue[F, Int] = parseOpt(opts.thresholdsLow)
  override def thresholdsBreak: ConfigValue[F, Int] = parseOpt(opts.thresholdsBreak)

  override def dashboardBaseUrl: ConfigValue[F, Uri] = parseOpt(opts.dashboardBaseUrl).as[Uri]
  override def dashboardReportType: ConfigValue[F, DashboardReportType] =
    parseOpt(opts.dashboardReportType).as[DashboardReportType]
  override def dashboardProject: ConfigValue[F, Option[String]] = parseOpt(opts.dashboardProject)
  override def dashboardVersion: ConfigValue[F, Option[String]] = parseOpt(opts.dashboardVersion)
  override def dashboardModule: ConfigValue[F, Option[String]] = parseOpt(opts.dashboardModule)

  override def timeout: ConfigValue[F, FiniteDuration] = parseOpt(opts.timeout)
  override def timeoutFactor: ConfigValue[F, Double] = parseOpt(opts.timeoutFactor)

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = parseOpt(opts.maxTestRunnerReuse)

  override def legacyTestRunner: ConfigValue[F, Boolean] = parseOpt(opts.legacyTestRunner)

  override def scalaDialect: ConfigValue[F, Dialect] = parseOpt(opts.scalaDialect).as[Dialect]

  override def concurrency: ConfigValue[F, Int] = parseOpt(opts.concurrency)

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] = parseOpt(opts.debugLogTestRunnerStdout)
  override def debugDebugTestRunner: ConfigValue[F, Boolean] = parseOpt(opts.debugDebugTestRunner)

  override def staticTmpDir: ConfigValue[F, Boolean] = parseOpt(opts.staticTmpDir)

  override def cleanTmpDir: ConfigValue[F, Boolean] = parseOpt(opts.cleanTmpDir)

  override def testRunnerCommand: ConfigValue[F, String] = parseOpt(opts.testRunnerCommand)
  override def testRunnerArgs: ConfigValue[F, String] = parseOpt(opts.testRunnerArgs)

  override def openReport: ConfigValue[F, Boolean] = parseOpt(opts.openReport)

  override def showHelpMessage: ConfigValue[F, Option[String]] = parseOpt(opts.help)

  /* All decline Opts for the CLI.
   * Separately defined so we can build a CLI help message with all options.
   */
  private object opts {

    val mutate: Opts[Seq[String]] =
      Opts.options[String]("mutate", short = "m", help = "A glob expression of files to mutate.").map(_.toList)

    val testFilter: Opts[Seq[String]] =
      Opts.options[String]("test-filter", short = "t", help = "A glob expression of tests to run.").map(_.toList)

    val baseDir: Opts[Path] =
      Opts.option[JPath]("base-dir", short = "b", help = "The root of the project.").map(Path.fromNioPath)

    val reporters: Opts[Seq[String]] =
      Opts
        .options[String]("reporters", short = "r", help = "The reporters to use.")
        .map(_.toList)

    val files: Opts[Seq[String]] =
      Opts
        .options[String]("files", short = "f", help = "The files to mutate.")
        .map(_.toList)

    val excludedMutations: Opts[Seq[String]] = Opts
      .options[String]("excluded-mutations", short = "e", help = "The mutations to exclude.")
      .map(_.toList)

    val thresholdsHigh: Opts[Int] = Opts.option[Int]("thresholds.high", help = "The high threshold.")
    val thresholdsLow: Opts[Int] = Opts.option[Int]("thresholds.low", help = "The low threshold.")
    val thresholdsBreak: Opts[Int] = Opts.option[Int]("thresholds.break", help = "The break threshold.")

    val dashboardBaseUrl: Opts[Uri] =
      Opts.option[java.net.URI]("dashboard.base-url", help = "The base url of the dashboard.").map(Uri(_))
    val dashboardReportType: Opts[String] =
      Opts.option[String]("dashboard.report-type", help = "The report type of the dashboard.")
    val dashboardProject: Opts[Option[String]] =
      Opts.option[String]("dashboard.project", help = "The project name of the dashboard.").map(_.some)
    val dashboardVersion: Opts[Option[String]] =
      Opts.option[String]("dashboard.version", help = "The version of the dashboard.").map(_.some)
    val dashboardModule: Opts[Option[String]] =
      Opts.option[String]("dashboard.module", help = "The module of the dashboard.").map(_.some)

    val timeout: Opts[FiniteDuration] =
      Opts.option[FiniteDuration]("timeout", short = "T", help = "The timeout for a test run.")
    val timeoutFactor: Opts[Double] =
      Opts.option[Double]("timeout-factor", short = "F", help = "The factor to multiply the timeout with.")

    val maxTestRunnerReuse: Opts[Option[Int]] = Opts
      .option[Int](
        "max-test-runner-reuse",
        help = "The maximum amount of times a test runner can be reused."
      )
      .map(_.some)

    val legacyTestRunner: Opts[Boolean] =
      Opts.flag("legacy-test-runner", help = "Use the legacy test runner.").as(true)

    val scalaDialect: Opts[String] =
      Opts.option[String]("scala-dialect", help = "The scala dialect to use.")

    val concurrency: Opts[Int] =
      Opts.option[Int]("concurrency", short = "c", help = "The amount of concurrent test runners.")

    val debugLogTestRunnerStdout: Opts[Boolean] = Opts
      .flag("log-test-runner-stdout", help = "Log the test runner stdout.")
      .as(true)

    val debugDebugTestRunner: Opts[Boolean] = Opts
      .flag("debug-test-runner", help = "Debug the test runner.")
      .as(true)

    val staticTmpDir: Opts[Boolean] = Opts
      .flag("static-tmp-dir", help = "Use a static tmp directory.")
      .as(true)

    val cleanTmpDir: Opts[Boolean] = Opts
      .flag("clean-tmp-dir", help = "Clean the tmp directory.")
      .as(true)

    val testRunnerCommand: Opts[String] = Opts
      .option[String]("test-runner-command", help = "The test runner command.")
    val testRunnerArgs: Opts[String] = Opts
      .option[String]("test-runner-args", help = "The test runner arguments.")

    val openReport: Opts[Boolean] =
      Opts.flag("open-report", short = "o", help = "Opens the report in the default browser.").as(true)

    // Same as Opts.help, but without `.asHelp`
    val help: Opts[Option[String]] =
      Opts.flag("help", help = "Display this help text.").map(_ => strykerCommand(opts.all).showHelp.some)

    // Used for the help message
    def all = List(
      testFilter,
      mutate,
      baseDir,
      reporters,
      files,
      excludedMutations,
      thresholdsHigh,
      thresholdsLow,
      thresholdsBreak,
      dashboardBaseUrl,
      dashboardReportType,
      dashboardProject,
      dashboardVersion,
      dashboardModule,
      timeout,
      timeoutFactor,
      maxTestRunnerReuse,
      legacyTestRunner,
      scalaDialect,
      concurrency,
      debugLogTestRunnerStdout,
      debugDebugTestRunner,
      staticTmpDir,
      cleanTmpDir,
      testRunnerCommand,
      testRunnerArgs,
      openReport,
      help
    ).sequenceVoid

    def strykerCommand[T]: Opts[T] => Command[T] = Command[T](
      name = "stryker4s",
      header = "Stryker4s - A mutation testing tool for Scala",
      helpFlag = false
    )

  }
}
