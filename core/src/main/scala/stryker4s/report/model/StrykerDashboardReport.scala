package stryker4s.report.model

import io.circe.Encoder

case class StrykerDashboardReport(apiKey: String, repositorySlug: String, branch: String, mutationScore: Double) {
  def toJson: String = StrykerDashboardReport.toJson(this)
}

object StrykerDashboardReport {
  def toJson(report: StrykerDashboardReport): String = {
    import stryker4s.report.model.StrykerDashboardReportEncoder._
    import io.circe.syntax._
    report.asJson.noSpaces
  }
}

protected object StrykerDashboardReportEncoder {
  implicit val dashboardReportEncoder: Encoder[StrykerDashboardReport] =
    Encoder.forProduct4("apiKey", "repositorySlug", "branch", "mutationScore")(
      d => (d.apiKey, d.repositorySlug, d.branch, d.mutationScore)
    )
}
