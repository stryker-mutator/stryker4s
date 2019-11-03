package stryker4s.report.model

import mutationtesting.MutationTestReport
import io.circe.Encoder
import io.circe.syntax._

sealed trait StrykerDashboardReport

case class FullDashboardReport(result: MutationTestReport) extends StrykerDashboardReport
case class ScoreOnlyReport(mutationScore: Double) extends StrykerDashboardReport

object StrykerDashboardReport {
  def toJson(report: StrykerDashboardReport): String = report match {
    case report: FullDashboardReport =>
      import mutationtesting.MutationReportEncoder._
      implicit val encoder: Encoder[FullDashboardReport] = Encoder.forProduct1("result")(r => r.result)
      report.asJson.noSpaces
    case report: ScoreOnlyReport =>
      implicit val encoder: Encoder[ScoreOnlyReport] = Encoder.forProduct1("mutationScore")(r => r.mutationScore)
      report.asJson.noSpaces
  }
}
