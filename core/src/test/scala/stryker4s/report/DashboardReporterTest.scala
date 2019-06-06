package stryker4s.report

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import stryker4s.config.Config
import stryker4s.http.{WebIO, WebResponse}
import stryker4s.model.MutantRunResults
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

import scala.concurrent.duration._

class DashboardReporterTest extends Stryker4sSuite with MockitoSugar with ArgumentMatchersSugar with LogMatchers {

  describe("reportJson") {
    it("should contain the report") {
      implicit val config: Config = Config()
      val mockWebIO = mock[WebIO]
      val sut = new DashboardReporter(mockWebIO)
      val testUrl = "http://targetUrl.com"
      val mutationScore = 22.0
      val durationMinutes = 10
      val runResults = MutantRunResults(
        results = Seq(),
        mutationScore = mutationScore,
        duration = durationMinutes.minutes
      )

      val convertToReport = sut.buildScoreResult(runResults)
      sut.writeReportToDashboard(testUrl, convertToReport)

      val expectedJs =
        s"""{"apiKey":"PLACEHOLDER_API_KEY","repositorySlug":"PLACEHOLDER_REPO_SLUG","branch":"PLACEHOLDER_BRANCH_NAME","mutationScore":$mutationScore}""".stripMargin

      verify(mockWebIO).postRequest(testUrl, expectedJs)
    }
  }

  describe("reportRunFinished") {
    implicit val config: Config = Config()

    it("should info log a message") {
      val sut = new DashboardReporter(new WebIO {
        override def postRequest(
            url: String,
            content: String
        ): WebResponse = {
          WebResponse(
            200,
            "Success query"
          )
        }
      })
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      "Written report to Dashboard" shouldBe loggedAsInfo
    }
  }

  describe("failing http") {
    implicit val config: Config = Config()

    it("should display errors") {
      val errorMessage = "HTTP have failed"
      val failingWebIO = new WebIO {
        override def postRequest(
            url: String,
            content: String
        ): WebResponse = {
          WebResponse(
            httpCode = 555,
            responseBody = errorMessage
          )
        }
      }
      val sut = new DashboardReporter(failingWebIO)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      errorMessage shouldBe loggedAsError
    }
  }
}
