package stryker4s.report.model
import io.circe.{Encoder, Json}
import stryker4s.report.model.MutantStatus.MutantStatus

final case class MutationTestReport(schemaVersion: String,
                                    thresholds: Thresholds,
                                    files: Map[String, MutationTestResult]) {

  private implicit val encoder: Encoder[MutantStatus] = Encoder.enumEncoder(MutantStatus)

  def toJson: Json = {
    import io.circe.generic.auto._
    import io.circe.syntax._
    this.asJson
  }
}
