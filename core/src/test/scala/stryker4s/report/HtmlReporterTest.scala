package stryker4s.report

import grizzled.slf4j.Logging
import org.json.JSONObject
import stryker4s.testutil.{MutationTestingElementsJsonSchema, Stryker4sSuite}

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

  describe("html reporter output validation") {
    it("should validate to the `mutation-testing-elements` json schema.") {
      schema.validate(new JSONObject(testReport))
    }
  }

}
