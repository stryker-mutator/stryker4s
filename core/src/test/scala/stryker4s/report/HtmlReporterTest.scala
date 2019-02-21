package stryker4s.report

import java.nio.file.Paths

import grizzled.slf4j.Logging
import org.everit.json.schema.ValidationException
import org.json.JSONObject
import stryker4s.config.Config
import stryker4s.extension.mutationtype.EmptyString
import stryker4s.model.{Killed, Mutant, MutantRunResults, Survived}
import stryker4s.report.mapper.MutantRunResultMapper
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
      |    "schemaVersion": "1",
      |    "thresholds": {
      |        "high": 80,
      |        "low": 60
      |    },
      |    "files": {
      |        "src/module1/Example.cs": {
      |            "language": "cs",
      |            "source": "using System; using.....",
      |            "mutants": [{
      |                "id": "321321",
      |                "mutatorName": "BinaryMutator",
      |                "replacement": "-",
      |                "location": {
      |                    "start": {
      |                        "line": 4,
      |                        "column": 3
      |                    },
      |                    "end": {
      |                        "line": 5,
      |                        "column": 2
      |                    }
      |                },
      |                "status": "Killed"
      |            }]
      |        }
      |    }
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

      val result = new JSONObject(sut.toJsonReport(mutationRunResults).spaces2)

      try {
        schema.validate(result)
      } catch {
        case exc: ValidationException =>
          // For testing purposes, to log failing validations
          fail(s"ValidationException occurred: ${exc.getAllMessages}", exc)
      }
    }
  }

  describe("html reporter output validation") {
    it("should validate to the `mutation-testing-elements` json schema.") {
      schema.validate(new JSONObject(testReport))
    }
  }

}
