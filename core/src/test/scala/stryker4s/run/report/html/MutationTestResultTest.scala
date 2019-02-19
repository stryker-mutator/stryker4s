package stryker4s.run.report.html
import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._
import org.everit.json.schema.ValidationException
import org.json.JSONObject
import stryker4s.run.report.html.MutationResultHealth.MutationResultHealth
import stryker4s.testutil.{MutationTestingElementsJsonSchema, Stryker4sSuite}
class MutationTestResultTest extends Stryker4sSuite {

  describe("created json") {
    it("should be valid according to mutation-report-schema") {
      val schema = MutationTestingElementsJsonSchema.mutationTestingElementsJsonSchema
      val sut = DirectoryResult(name = "stryker4s-core",
                                mutationScore = 100,
                                health = MutationResultHealth.ok,
                                totals = ResultTotals(10, 10, 10, 0),
        path = "",
                                childResults = Seq(

                                  ))
      implicit val encoder: Encoder[MutationResultHealth] = io.circe.Encoder.enumEncoder(MutationResultHealth)
      val result = new JSONObject(sut.asJson.toString())

      try {
        schema.validate(result)
      } catch {
        case exc: ValidationException =>
          // For testing purposes, to log failing validations
          fail(s"ValidationException occurred: ${exc.getAllMessages}", exc)
      }
    }
  }

}
