package stryker4s.report

import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.http.{WebIO, WebResponse}
import stryker4s.model.MutantRunResults
import stryker4s.report.mapper.MutantRunResultMapper

class DashboardReporter(webHelper: WebIO)(implicit config: Config)
    extends FinishedRunReporter
    with MutantRunResultMapper
    with Logging {

  // https://github.com/stryker-mutator/stryker/blob/master/packages/core/src/reporters/dashboard-reporter/DashboardReporterClient.ts
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

  private val DashboardRootURL: String = "https://dashboard.stryker-mutator.io"
  private val DashboardURL: String = s"$DashboardRootURL/api/reports"

  def buildScoreResult(input: MutantRunResults): StrykerDashboardReport = {
    StrykerDashboardReport(
      "PLACEHOLDER_API_KEY", // TODO: Get apikey
      "PLACEHOLDER_REPO_SLUG", // TODO: Get repo slug
      "PLACEHOLDER_BRANCH_NAME", // TODO: Get branch name
      input.mutationScore
    )
  }

  def writeReportToDashboard(url: String, report: StrykerDashboardReport): WebResponse = {
    webHelper.postRequest(url, report.toJson)
  }

  override def reportRunFinished(runResults: MutantRunResults): Unit = {
    val response = writeReportToDashboard(DashboardURL, buildScoreResult(runResults))

    if (!response.isSuccess) {
      error(s"Failed to write to dashboard\nError: ${response.httpCode}\n Body was: \n${response.responseBody}")
    }

    info(s"Written report to Dashboard: $DashboardRootURL")
  }
}
