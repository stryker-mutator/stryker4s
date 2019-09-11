package stryker4s.report.model
import io.circe.Encoder
import stryker4s.report.model.MutantStatus.MutantStatus

final case class MutationTestReport(
    schemaVersion: String,
    thresholds: Thresholds,
    files: Map[String, MutationTestResult]
) {

  def toJson: String = {
    implicit val encoder: Encoder[MutantStatus] = Encoder.encodeEnumeration(MutantStatus)
    import io.circe.generic.auto._
    import io.circe.syntax._
    this.asJson.noSpaces
  }
}

final case class MutationTestResult(source: String, mutants: Seq[MutantResult], language: String = "scala")

final case class MutantResult(
    id: String,
    mutatorName: String,
    replacement: String,
    location: Location,
    status: MutantStatus
)

final case class Location(start: Position, end: Position)

final case class Position(line: Int, column: Int)

final case class Thresholds(high: Int, low: Int)

object MutantStatus extends Enumeration {
  type MutantStatus = Value
  val Killed, Survived, NoCoverage, CompileError, Timeout = Value
}
