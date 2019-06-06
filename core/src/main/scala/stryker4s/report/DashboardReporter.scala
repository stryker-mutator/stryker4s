package stryker4s.report

import grizzled.slf4j.Logging
import stryker4s.config.{Config, DashboardReporterType, ReporterType}
import stryker4s.http.{RealHttp, WebIO, WebResponse}
import stryker4s.model.MutantRunResults
import stryker4s.report.dashboard.Providers._
import stryker4s.report.mapper.MutantRunResultMapper

class DashboardReporter(webIO: WebIO, ciEnvironment: CiEnvironment)(implicit config: Config)
    extends FinishedRunReporter
    with MutantRunResultMapper
    with Logging {

  case class StrykerDashboardReport(
      apiKey: String,
      repositorySlug: String,
      branch: String,
      mutationScore: Double
  ) {

    def toJson: String = {
      import io.circe.generic.auto._
      import io.circe.syntax._
      this.asJson.noSpaces
    }
  }

  private val dashboardRootURL: String = "https://dashboard.stryker-mutator.io"
  private val dashboardURL: String = s"$dashboardRootURL/api/reports"

  def buildScoreResult(input: MutantRunResults): StrykerDashboardReport = {
    StrykerDashboardReport(
      ciEnvironment.apiKey,
      ciEnvironment.repository,
      ciEnvironment.branchName,
      input.mutationScore
    )
  }

  def writeReportToDashboard(url: String, report: StrykerDashboardReport): WebResponse = {
    webIO.postRequest(url, report.toJson)
  }

  override def reportRunFinished(runResults: MutantRunResults): Unit = {
    val response = writeReportToDashboard(dashboardURL, buildScoreResult(runResults))

    if (response.httpCode == 201) {
      info(s"Sent report to Dashboard: $dashboardRootURL")
    } else {
      error(s"Failed to send report to dashboard.")
      error(s"Code: ${response.httpCode}. Body: '${response.responseBody}'")
    }
  }
}

object DashboardReporter {

  def unapply(reporterType: ReporterType)(implicit config: Config): Option[DashboardReporter] = reporterType match {
    case DashboardReporterType => resolveProvider()
    case _                     => None
  }

  def resolveProvider()(implicit config: Config): Option[DashboardReporter] =
    resolveCiEnvironment()
      .map(new DashboardReporter(RealHttp, _))

  def resolveCiEnvironment(): Option[CiEnvironment] =
    tryResolveEnv(TravisProvider) orElse
      tryResolveEnv(CircleProvider)

  private def tryResolveEnv(provider: CiProvider): Option[CiEnvironment] =
    for {
      apiKey <- provider.determineApiKey()
      branchName <- provider.determineBranch()
      repoName <- provider.determineRepository()
      if !provider.isPullRequest
    } yield CiEnvironment(apiKey, repoName, branchName)

}

case class CiEnvironment(apiKey: String, repository: String, branchName: String)
