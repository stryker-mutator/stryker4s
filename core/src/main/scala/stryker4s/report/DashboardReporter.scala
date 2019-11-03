package stryker4s.report

import grizzled.slf4j.Logging
import mutationtesting.{MetricsResult, MutationTestReport}
import scalaj.http.HttpResponse
import stryker4s.http.WebIO
import stryker4s.report.dashboard.Providers._
import stryker4s.report.model._
import stryker4s.config.Config
import stryker4s.config.Full
import stryker4s.config.MutationScoreOnly
import io.circe.parser.decode
import io.circe.Decoder
class DashboardReporter(webIO: WebIO, ciEnvironment: Option[CiEnvironment])(implicit config: Config)
    extends FinishedRunReporter
    with Logging {
  def buildUrl: Option[String] =
    for {
      project <- config.dashboard.project.orElse(ciEnvironment.map(_.project))
      version <- config.dashboard.version.orElse(ciEnvironment.map(_.version))
      baseUrl = config.dashboard.baseUrl
      url = s"$baseUrl/api/reports/$project/$version"
    } yield config.dashboard.module match {
      case Some(module) => s"$url?module=$module"
      case None         => url
    }

  def buildBody(report: MutationTestReport, metrics: MetricsResult): StrykerDashboardReport = {
    config.dashboard.reportType match {
      case Full              => FullDashboardReport(report)
      case MutationScoreOnly => ScoreOnlyReport(metrics.mutationScore)
    }
  }

  def writeReportToDashboard(url: String, body: StrykerDashboardReport, apiKey: String): HttpResponse[String] = {
    webIO.putRequest(url, StrykerDashboardReport.toJson(body), Map("X-Api-Key" -> apiKey))
  }

  override def reportRunFinished(report: MutationTestReport, metrics: MetricsResult): Unit = {
    buildUrl match {
      case None => info("Could not resolve dashboard configuration, not sending report")
      case Some(url) =>
        val body = buildBody(report, metrics)
        val response = writeReportToDashboard(url, body, ciEnvironment.get.apikey)

        if (response.code == 200) {
          if (config.dashboard.reportType == Full) {
            implicit val decoder: Decoder[Href] = Decoder.forProduct1("href")(Href.apply)
            val href = decode[Href](response.body) match {
              case Left(error)        => throw error
              case Right(Href(value)) => value
            }
            info(s"Sent report to dashboard: $href")
          } else {
            info(s"Sent report to dashboard: $url")
          }
        } else {
          error(s"Failed to send report to dashboard.")
          error(s"Expected status code 200, but was ${response.code}. Body: '${response.body}'")
        }
    }
  }
}

object DashboardReporter {
  def resolveCiEnvironment(): Option[CiEnvironment] =
    tryResolveEnv(TravisProvider) orElse
      tryResolveEnv(CircleProvider)

  def tryResolveEnv(provider: CiProvider): Option[CiEnvironment] =
    if (provider.isPullRequest) None
    else
      for {
        apiKey <- provider.determineApiKey()
        project <- provider.determineProject()
        withGitHub = s"github.com/$project"
        version <- provider.determineVersion()
      } yield CiEnvironment(apiKey, withGitHub, version)
}

case class CiEnvironment(apikey: String, project: String, version: String)

case class Href(href: String)
