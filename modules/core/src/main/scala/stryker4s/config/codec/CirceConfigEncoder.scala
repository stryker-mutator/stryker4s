package stryker4s.config.codec

import fs2.io.file.Path
import io.circe.Encoder
import stryker4s.config.{Config, *}
import sttp.model.Uri

import scala.concurrent.duration.FiniteDuration
import scala.meta.Dialect

/** Circe Encoder for encoding a [[stryker4s.config.Config]] to JSON
  */
trait CirceConfigEncoder {
  implicit def configEncoder: Encoder[Config] = Encoder
    .forProduct14(
      "mutate",
      "test-filter",
      "base-dir",
      "reporters",
      "files",
      "excluded-mutations",
      "thresholds",
      "dashboard",
      "timeout",
      "timeout-factor",
      "max-test-runner-reuse",
      "legacy-test-runner",
      "scala-dialect",
      "debug"
    )((c: Config) =>
      (
        c.mutate,
        c.testFilter,
        c.baseDir,
        c.reporters,
        c.files,
        c.excludedMutations,
        c.thresholds,
        c.dashboard,
        c.timeout,
        c.timeoutFactor,
        c.maxTestRunnerReuse,
        c.legacyTestRunner,
        c.scalaDialect,
        c.debug
      )
    )
    .mapJson(_.deepDropNullValues)

  implicit def pathEncoder: Encoder[Path] = Encoder[String].contramap(_.absolute.toString)
  implicit def reporterTypeEncoder: Encoder[ReporterType] = Encoder[String].contramap(_.toString.toLowerCase)
  implicit def thresholdsEncoder: Encoder[Thresholds] =
    Encoder.forProduct3("high", "low", "break")(t => (t.high, t.low, t.break))

  implicit def dashboardOptionsEncoder: Encoder[DashboardOptions] = Encoder.forProduct5(
    "base-url",
    "report-type",
    "project",
    "version",
    "module"
  )(d =>
    (
      d.baseUrl,
      d.reportType,
      d.project,
      d.version,
      d.module
    )
  )

  implicit def finiteDurationEncoder: Encoder[FiniteDuration] = Encoder[Long].contramap(_.toMillis)

  implicit def uriEncoder: Encoder[Uri] = Encoder[String].contramap(_.toString())

  implicit def reportTypeEncoder: Encoder[DashboardReportType] = Encoder[String].contramap(_.toString.toLowerCase)

  implicit def dialectEncoder: Encoder[Dialect] = Encoder[String].contramap(_.toString.toLowerCase)

  implicit def debugOptionsEncoder: Encoder[DebugOptions] =
    Encoder.forProduct2("log-test-runner-stdout", "debug-test-runner")(d => (d.logTestRunnerStdout, d.debugTestRunner))

  implicit def excludedMutationEncoder: Encoder[ExcludedMutation] = Encoder[String].contramap(_.value)
}
