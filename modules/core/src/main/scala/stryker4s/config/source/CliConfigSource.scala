package stryker4s.config.source

import cats.syntax.all.*
import ciris.*
import fs2.io.file.Path
import scopt.{DefaultOParserSetup, OEffect, OParser, OParserBuilder}
import stryker4s.config.codec.CirisConfigDecoders
import stryker4s.config.{ConfigOrder, DashboardReportType, ExcludedMutation, ReporterType}
import sttp.model.Uri

import java.nio.file.Path as JPath
import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect

class CliConfigSource[F[_]](args: Seq[String]) extends ConfigSource[F] with CirisConfigDecoders {

  private def parseOpt[A, T](
      parser: OParser[A, Option[T]]
  )(implicit name: sourcecode.Name): ConfigValue[F, T] = {
    val parserSetup = new DefaultOParserSetup {
      override def errorOnUnknownArgument: Boolean = false
    }
    val parsed = OParser.runParser(parser, args, none, parserSetup)

    parsed match {
      case (Some(Some(value)), _) => ConfigValue.loaded(ConfigKey(name.value), value)
      case (_, errs) if errs.nonEmpty =>
        errs
          .map {
            case OEffect.DisplayToErr(msg)    => ConfigValue.missing[T](msg)
            case OEffect.DisplayToOut(msg)    => ConfigValue.missing[T](msg)
            case OEffect.ReportError(msg)     => ConfigValue.missing[T](msg)
            case OEffect.ReportWarning(msg)   => ConfigValue.missing[T](msg)
            case OEffect.Terminate(exitState) => ConfigValue.failed[T](ConfigError(s"Terminate: $exitState"))
          }
          .reduce(_ or _)
      case (_, _) => ConfigValue.missing(name.value + " option")
    }
  }

  override def name: String = "CLI arguments"

  override def priority: ConfigOrder = ConfigOrder(5)

  override def mutate: ConfigValue[F, Seq[String]] = parseOpt(opts.mutate)

  override def testFilter: ConfigValue[F, Seq[String]] = parseOpt(opts.testFilter)

  override def baseDir: ConfigValue[F, Path] = parseOpt(opts.baseDir).map(Path.fromNioPath)

  override def reporters: ConfigValue[F, Seq[ReporterType]] = parseOpt(opts.reporters).as[Seq[ReporterType]]

  override def files: ConfigValue[F, Seq[String]] = parseOpt(opts.files)

  override def excludedMutations: ConfigValue[F, Seq[ExcludedMutation]] =
    parseOpt(opts.excludedMutations).as[Seq[ExcludedMutation]]

  override def thresholdsHigh: ConfigValue[F, Int] = parseOpt(opts.thresholdsHigh)
  override def thresholdsLow: ConfigValue[F, Int] = parseOpt(opts.thresholdsLow)
  override def thresholdsBreak: ConfigValue[F, Int] = parseOpt(opts.thresholdsBreak)

  override def dashboardBaseUrl: ConfigValue[F, Uri] = parseOpt(opts.dashboardBaseUrl).map(Uri(_))
  override def dashboardReportType: ConfigValue[F, DashboardReportType] =
    parseOpt(opts.dashboardReportType).as[DashboardReportType]
  override def dashboardProject: ConfigValue[F, Option[String]] = parseOpt(opts.dashboardProject).map(_.some)
  override def dashboardVersion: ConfigValue[F, Option[String]] = parseOpt(opts.dashboardVersion).map(_.some)
  override def dashboardModule: ConfigValue[F, Option[String]] = parseOpt(opts.dashboardModule).map(_.some)

  override def timeout: ConfigValue[F, FiniteDuration] = parseOpt(opts.timeout)
  override def timeoutFactor: ConfigValue[F, Double] = parseOpt(opts.timeoutFactor)

  override def maxTestRunnerReuse: ConfigValue[F, Option[Int]] = parseOpt(opts.maxTestRunnerReuse).map(_.some)

  override def legacyTestRunner: ConfigValue[F, Boolean] = parseOpt(opts.legacyTestRunner).map(_ => true)

  override def scalaDialect: ConfigValue[F, Dialect] = parseOpt(opts.scalaDialect).as[Dialect]

  override def concurrency: ConfigValue[F, Int] = parseOpt(opts.concurrency)

  override def debugLogTestRunnerStdout: ConfigValue[F, Boolean] =
    parseOpt(opts.debugLogTestRunnerStdout).map(_ => true)
  override def debugDebugTestRunner: ConfigValue[F, Boolean] = parseOpt(opts.debugDebugTestRunner).map(_ => true)

  override def staticTmpDir: ConfigValue[F, Boolean] = parseOpt(opts.staticTmpDir).map(_ => true)

  override def cleanTmpDir: ConfigValue[F, Boolean] = parseOpt(opts.cleanTmpDir).map(_ => true)

  override def testRunnerCommand: ConfigValue[F, String] = parseOpt(opts.testRunnerCommand)
  override def testRunnerArgs: ConfigValue[F, String] = parseOpt(opts.testRunnerArgs)

  override def openReport: ConfigValue[F, Boolean] = parseOpt(opts.openReport).map(_ => true)

  override def showHelpMessage: ConfigValue[F, Option[String]] =
    parseOpt(opts.help).map(_ => opts.helpText.some)

  /* All decline Opts for the CLI.
   * Separately defined so we can build a CLI help message with all options.
   */
  private object opts {
    private def makeOpt[T](f: OParserBuilder[Option[T]] => OParser[T, Option[T]]) =
      f(OParser.builder[Option[T]])
        .action((x, _) => x.some)

    private def makeRepeatOpt[T](f: OParserBuilder[Option[Seq[T]]] => OParser[T, Option[Seq[T]]]) =
      f(OParser.builder[Option[Seq[T]]])
        .unbounded()
        .action((x, acc) => acc.map(_ :+ x).orElse(Seq(x).some))

    val mutate = makeRepeatOpt[String](_.opt[String]('m', "mutate").text("A glob expression of files to mutate."))

    val testFilter = makeRepeatOpt[String](_.opt[String]('t', "test-filter").text("A glob expression of tests to run."))

    val baseDir = makeOpt[JPath](_.opt[JPath]('b', "base-dir").text("The root of the project."))

    val reporters = makeRepeatOpt[String](_.opt[String]('r', "reporters").text("The reporters to use."))

    val files = makeRepeatOpt[String](_.opt[String]('f', "files").text("The files to mutate."))

    val excludedMutations =
      makeRepeatOpt[String](_.opt[String]('e', "excluded-mutations").text("The mutations to exclude."))

    val thresholdsHigh = makeOpt[Int](_.opt[Int]("thresholds.high").text("The high threshold."))
    val thresholdsLow = makeOpt[Int](_.opt[Int]("thresholds.low").text("The low threshold."))
    val thresholdsBreak = makeOpt[Int](_.opt[Int]("thresholds.break").text("The break threshold."))

    val dashboardBaseUrl =
      makeOpt[java.net.URI](_.opt[java.net.URI]("dashboard.base-url").text("The base url of the dashboard."))
    val dashboardReportType =
      makeOpt[String](_.opt[String]("dashboard.report-type").text("The report type of the dashboard."))
    val dashboardProject =
      makeOpt[String](_.opt[String]("dashboard.project").text("The project name of the dashboard."))
    val dashboardVersion = makeOpt[String](_.opt[String]("dashboard.version").text("The version of the dashboard."))
    val dashboardModule = makeOpt[String](_.opt[String]("dashboard.module").text("The module of the dashboard."))

    val timeout = makeOpt[FiniteDuration](_.opt[FiniteDuration]('T', "timeout").text("The timeout for a test run."))
    val timeoutFactor =
      makeOpt[Double](_.opt[Double]('F', "timeout-factor").text("The factor to multiply the timeout with."))

    val maxTestRunnerReuse =
      makeOpt[Int](_.opt[Int]("max-test-runner-reuse").text("The maximum amount of times a test runner can be reused."))

    val legacyTestRunner = makeOpt[Unit](_.opt[Unit]("legacy-test-runner").text("Use the legacy test runner."))

    val scalaDialect = makeOpt[String](_.opt[String]("scala-dialect").text("The scala dialect to use."))

    val concurrency = makeOpt[Int](_.opt[Int]('c', "concurrency").text("The amount of concurrent test runners."))

    val debugLogTestRunnerStdout =
      makeOpt[Unit](_.opt[Unit]("log-test-runner-stdout").text("Log the test runner stdout."))

    val debugDebugTestRunner = makeOpt[Unit](_.opt[Unit]("debug-test-runner").text("Debug the test runner."))

    val staticTmpDir = makeOpt[Unit](_.opt[Unit]("static-tmp-dir").text("Use a static tmp directory."))

    val cleanTmpDir = makeOpt[Unit](_.opt[Unit]("clean-tmp-dir").text("Clean the tmp directory."))

    val testRunnerCommand = makeOpt[String](_.opt[String]("test-runner-command").text("The test runner command."))
    val testRunnerArgs = makeOpt[String](_.opt[String]("test-runner-args").text("The test runner arguments."))

    val openReport =
      makeOpt[Unit](_.opt[Unit]('o', "open-report").text("Opens the report in the default browser."))

    val help = makeOpt[Unit](_.opt[Unit]("help")).text("Display this help text.")

    def helpText = {
      def asAnyP[A, C](p: OParser[A, Option[C]]): OParser[Any, Option[Any]] = p.asInstanceOf[OParser[Any, Option[Any]]]
      OParser
        .usage(
          OParser.sequence(
            asAnyP(OParser.builder[Option[Unit]].programName("stryker4s")),
            asAnyP(OParser.builder[Option[Unit]].note("Stryker4s - A mutation testing tool for Scala")),
            asAnyP(testFilter),
            asAnyP(mutate),
            asAnyP(baseDir),
            asAnyP(reporters),
            asAnyP(files),
            asAnyP(excludedMutations),
            asAnyP(thresholdsHigh),
            asAnyP(thresholdsLow),
            asAnyP(thresholdsBreak),
            asAnyP(dashboardBaseUrl),
            asAnyP(dashboardReportType),
            asAnyP(dashboardProject),
            asAnyP(dashboardVersion),
            asAnyP(dashboardModule),
            asAnyP(timeout),
            asAnyP(timeoutFactor),
            asAnyP(maxTestRunnerReuse),
            asAnyP(legacyTestRunner),
            asAnyP(scalaDialect),
            asAnyP(concurrency),
            asAnyP(debugLogTestRunnerStdout),
            asAnyP(debugDebugTestRunner),
            asAnyP(staticTmpDir),
            asAnyP(cleanTmpDir),
            asAnyP(testRunnerCommand),
            asAnyP(testRunnerArgs),
            asAnyP(openReport),
            asAnyP(help)
          )
        )
    }

  }

}
