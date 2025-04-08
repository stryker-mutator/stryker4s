package stryker4s.report

import cats.data.Validated.{Invalid, Valid}
import cats.effect.{IO, Resource}
import cats.syntax.foldable.*
import fansi.Color.Red
import fansi.{Bold, Str}
import mutationtesting.{MetricsResult, MutationTestResult}
import stryker4s.config.codec.CirceConfigEncoder
import stryker4s.config.{Config, Full, MutationScoreOnly}
import stryker4s.log.Logger
import stryker4s.report.dashboard.DashboardConfigProvider
import stryker4s.report.model.*
import sttp.client4.*
import sttp.client4.ResponseException.{DeserializationException, UnexpectedStatusCode}
import sttp.client4.circe.*
import sttp.model.{MediaType, StatusCode}

class DashboardReporter(dashboardConfigProvider: DashboardConfigProvider[IO])(implicit
    log: Logger,
    httpBackend: Resource[IO, Backend[IO]]
) extends Reporter
    with CirceConfigEncoder {

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] =
    dashboardConfigProvider.resolveConfig().flatMap {
      case Invalid(configKeys) =>
        val configKeysStr = Str.join(configKeys.map(c => Str("'", Bold.On(c), "'")).toList, ", ")
        IO(log.warn(s"Could not resolve dashboard configuration key(s) $configKeysStr. Not sending report."))
      case Valid(dashboardConfig) =>
        val request = buildRequest(dashboardConfig, runReport.report, runReport.metrics)
        httpBackend
          .use(request.send(_))
          .map(logResponse(_))
    }

  def buildRequest(dashConfig: DashboardConfig, report: MutationTestResult[Config], metrics: MetricsResult) = {
    import io.circe.{Decoder, Encoder}
    implicit val decoder: Decoder[DashboardPutResult] = Decoder.forProduct1("href")(DashboardPutResult.apply)
    // Separate so any slashes won't be escaped in project or version
    val baseUrl = s"${dashConfig.baseUrl}/api/reports/${dashConfig.project}/${dashConfig.version}"
    val uri = uri"$baseUrl?module=${dashConfig.module}"
    val request = basicRequest
      .header("X-Api-Key", dashConfig.apiKey)
      .contentType(MediaType.ApplicationJson)
      .response(asJson[DashboardPutResult])
      .put(uri)
    dashConfig.reportType match {
      case Full =>
        import mutationtesting.circe.*
        request
          .body(asJson(report))
      case MutationScoreOnly =>
        implicit val encoder: Encoder[ScoreOnlyReport] = Encoder.forProduct1("mutationScore")(r => r.mutationScore)
        request
          .body(asJson(ScoreOnlyReport(metrics.mutationScore)))
    }
  }

  private def logResponse(response: Response[Either[ResponseException[String], DashboardPutResult]]): Unit =
    response.body match {
      case Left(UnexpectedStatusCode(errorBody, meta)) if meta.code == StatusCode.Unauthorized =>
        log.error(
          s"Error HTTP PUT '$errorBody'. Status code ${Red("401 Unauthorized")}. Did you provide the correct api key in the '${Bold
              .On("STRYKER_DASHBOARD_API_KEY")}' environment variable?"
        )
      case Left(UnexpectedStatusCode(errorBody, statusCode)) =>
        log.error(
          s"Failed to PUT report to dashboard. Response status code: ${Red(statusCode.code.toString())}. Response body: '$errorBody'"
        )
      case Left(DeserializationException(original, error, _)) =>
        log.warn(
          s"Dashboard report was sent successfully, but could not decode the response: '$original'. Error:",
          error
        )
      case Right(DashboardPutResult(href)) =>
        log.info(s"Sent report to dashboard. Available at $href")
    }
}
