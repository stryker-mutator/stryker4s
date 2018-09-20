package stryker4s.run.report

import java.util.concurrent.TimeUnit

import better.files.File
import grizzled.slf4j.Logging
import org.everit.json.schema.ValidationException
import org.json.JSONObject
import org.scalactic.Fail
import stryker4s.config.Config
import stryker4s.extensions.mutationtypes.{GreaterThan, LesserThan}
import stryker4s.model.{Killed, Mutant, MutantRunResults, Survived}
import stryker4s.{MutationTestingElementsJsonSchema, Stryker4sSuite}
import stryker4s.extensions.ImplicitMutationConversion._

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class HtmlReporterTest extends Stryker4sSuite with Logging {

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

  describe("") {
    it("") {
      val sut = new HtmlReporter
      implicit val config: Config = Config()

      val mutantRunResult = Killed(0, Mutant(0, GreaterThan, LesserThan, "test"), config.baseDir.relativize(File("/core\\src\\main\\scala\\stryker4s\\config.scala")))
      val mutantRunResult2 = Killed(0, Mutant(0, GreaterThan, LesserThan, "test"), config.baseDir.relativize(File("/to/a/file")))
      val mutantRunResult3 = Killed(0, Mutant(0, GreaterThan, LesserThan, "test"), config.baseDir.relativize(File("/to/a/file2")))
      val mutantRunResult4 = Survived(Mutant(0, GreaterThan, LesserThan, "test"), config.baseDir.relativize(File("/to/a/file3")))
      val mutationRunResults = MutantRunResults(List(mutantRunResult, mutantRunResult2, mutantRunResult3, mutantRunResult4), 100.0, Duration(10, TimeUnit.SECONDS))

      sut.report(mutationRunResults)
    }
  }

  describe("html reporter output validation") {
    it("should validate to the `mutation-testing-elements` json schema.") {
      MutationTestingElementsJsonSchema.mutationTestingElementsJsonSchema match {
        case Some(schema) =>
          Try(schema.validate(new JSONObject(testReport))) match {
            case Success(_)         => succeed
            case Failure(exception) => fail(exception.getMessage)
          }
        case None =>
          Fail("Schema could not be retrieved")
      }
    }

    it("should fail when an empty json string is provided because not all required elements are available.") {
      MutationTestingElementsJsonSchema.mutationTestingElementsJsonSchema match {
        case Some(schema) =>
          Try(schema.validate(new JSONObject("{}"))) match {
            case Success(_)         => fail("Should fail because input was not correct")
            case Failure(exception) =>
              val validationException = exception.asInstanceOf[ValidationException]

              validationException.getAllMessages should  contain ("#: required key [name] not found")
          }
        case None =>
          Fail("Schema could not be retrieved")
      }
    }
  }
}
