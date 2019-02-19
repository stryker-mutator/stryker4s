package stryker4s.run.report

import java.nio.file.Paths

import grizzled.slf4j.Logging
import org.everit.json.schema.ValidationException
import org.json.JSONObject
import stryker4s.config.Config
import stryker4s.extension.mutationtype.EmptyString
import stryker4s.model.{Killed, Mutant, MutantRunResults, Survived}
import stryker4s.run.report.mapper.MutantRunResultMapper
import stryker4s.testutil.{MutationTestingElementsJsonSchema, Stryker4sSuite}

import scala.concurrent.duration._
import scala.meta._

class HtmlReporterTest extends Stryker4sSuite with Logging {

  private val schema = MutationTestingElementsJsonSchema.mutationTestingElementsJsonSchema

  /**
    * Test report should be replaced by the html reporter output but this isn't available yet.
    */
  private[this] val testReport: String =
    """
      |{
      |    "name": "src",
      |    "path":  "/usr/full/path/to/src",
      |    "totals": {
      |       "choose": 8,
      |       "custom": 1,
      |       "columns": 1,
      |       "here": 0,
      |       "like": 2,
      |       "mutation score": 80
      |    },
      |    "health": "ok",
      |    "childResults": [
      |        {
      |            "name": "src/Example.cs",
      |            "path":  "/usr/full/path/to/src/Example.cs",
      |            "totals": {
      |               "detected": 1,
      |               "undetected": 2,
      |               "valid": 3,
      |               "invalid": 1
      |             },
      |            "health": "danger",
      |            "language": "cs",
      |            "source": "using System; using.....",
      |            "mutants": [{
      |                 "id": "321321",
      |                 "mutatorName": "BinaryMutator",
      |                 "replacement": "-",
      |                 "span": [21,22],
      |                 "status": "Killed"
      |            }]
      |        }
      |    ]
      |}
    """.stripMargin

  describe("mapper") {
    it("should map 4 files to valid JSON") {
      val sut = new MutantRunResultMapper {}
      implicit val config: Config = Config()

      val mutantRunResult = Killed(
        Mutant(0, q"4", q"5", EmptyString),
        Paths.get("core/src/main/scala/stryker4s/extension/mutationtype/BooleanLiteral.scala")
      )
      val mutantRunResult2 = Killed(
        Mutant(0, q"4", q"5", EmptyString),
        Paths.get("core/src/main/scala/stryker4s/extension/mutationtype/BooleanLiteral.scala")
      )
      val mutantRunResult3 = Killed(
        Mutant(0, q"4", q"5", EmptyString),
        Paths.get("core/src/main/scala/stryker4s/run/report/ConsoleReporter.scala")
      )
      val mutantRunResult4 = Survived(
        Mutant(0, q"<", q">", EmptyString),
        Paths.get("core/src/main/scala/stryker4s/run/report/mapper/MutantRunResultMapper.scala")
      )

      val mutationRunResults =
        MutantRunResults(List(mutantRunResult, mutantRunResult2, mutantRunResult3, mutantRunResult4), 100.0, 10.seconds)

      val result = sut.toHtmlMutantRunResult(mutationRunResults)
    }
  }

  describe("html reporter output validation") {
    it("should validate to the `mutation-testing-elements` json schema.") {
      schema.validate(new JSONObject(testReport))
    }
  }

  it("should fail when an empty json string is provided because not all required elements are available.") {
    val exception = the[ValidationException] thrownBy schema.validate(new JSONObject("{}"))

    exception.getAllMessages should contain("#: required key [name] not found")
  }
}
