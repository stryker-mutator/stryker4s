package stryker4s.report.model

case class StrykerDashboardReport(apiKey: String, repositorySlug: String, branch: String, mutationScore: Double) {

  def toJson: String = {
    import io.circe.generic.auto._
    import io.circe.syntax._
    this.asJson.noSpaces
  }
}
