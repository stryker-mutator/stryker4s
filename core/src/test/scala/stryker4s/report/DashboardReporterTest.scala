package stryker4s.report

import stryker4s.config.Config
import stryker4s.http.{WebIO, WebResponse}
import stryker4s.model.MutantRunResults
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoSuite, Stryker4sSuite}

import scala.concurrent.duration._

class DashboardReporterTest extends Stryker4sSuite with MockitoSuite with LogMatchers {

  val ciEnvironment = CiEnvironment("foo", "bar", "baz")
  describe("reportJson") {
    it("should contain the report") {
      implicit val config: Config = Config()
      val mockWebIO = mock[WebIO]
      val sut = new DashboardReporter(mockWebIO, ciEnvironment)
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
        s"""{"apiKey":"foo","repositorySlug":"bar","branch":"baz","mutationScore":$mutationScore}""".stripMargin

      verify(mockWebIO).postRequest(testUrl, expectedJs)
    }
  }

  describe("reportRunFinished") {
    implicit val config: Config = Config()

    it("should info log a message on success") {
      val sut = new DashboardReporter((_: String, _: String) => {
        WebResponse(
          201,
          "Success query"
        )
      }, ciEnvironment)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      "Sent report to Dashboard: https://dashboard.stryker-mutator.io" shouldBe loggedAsInfo
      "Failed t osend report to dashboard." should not be loggedAsError
    }

    it("should error log on failed HTTP call") {
      val sut = new DashboardReporter((_: String, _: String) => {
        WebResponse(
          400,
          "Bad request"
        )
      }, ciEnvironment)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)
      "Failed to send report to dashboard." shouldBe loggedAsError
      "Code: 400. Body: 'Bad request'" shouldBe loggedAsError
      "Sent report to Dashboard: https://dashboard.stryker-mutator.io" should not be loggedAsInfo

    }
  }
}
