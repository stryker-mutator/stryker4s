package stryker4s.report

import io.circe.generic.auto._
import io.circe.syntax._
import scalaj.http.HttpResponse
import stryker4s.config.Config
import stryker4s.http.WebIO
import stryker4s.model.MutantRunResults
import stryker4s.report.model.StrykerDashboardReport
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoSuite, Stryker4sSuite}
import stryker4s.report.dashboard.Providers.CiProvider
import scala.concurrent.duration._

class DashboardReporterTest extends Stryker4sSuite with MockitoSuite with LogMatchers {

  val ciEnvironment = CiEnvironment("someApiKey", "myRepo", "myBranch")
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

      val expectedJson = convertToReport.asJson.noSpaces
      convertToReport should equal(StrykerDashboardReport("someApiKey", "myRepo", "myBranch", mutationScore))
      verify(mockWebIO).postRequest(testUrl, expectedJson)
    }
  }

  describe("reportRunFinished") {
    implicit val config: Config = Config()
    val sentMessage = "Sent report to dashboard: https://dashboard.stryker-mutator.io"
    val failedMessage = "Failed to send report to dashboard."

    it("should info log a message on 201 success") {
      val sut = new DashboardReporter((_: String, _: String) => {
        HttpResponse("Success query", 201, Map.empty)
      }, ciEnvironment)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      sentMessage shouldBe loggedAsInfo
      failedMessage should not be loggedAsError
      "Expected status code 201, but was " should not be loggedAsError
    }

    it("should error log on anything other than a 201") {
      val sut = new DashboardReporter((_: String, _: String) => {
        HttpResponse("null", 200, Map.empty)
      }, ciEnvironment)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      failedMessage shouldBe loggedAsError
      "Expected status code 201, but was 200. Body: 'null'" shouldBe loggedAsError
      sentMessage should not be loggedAsInfo
    }

    it("should error log on failed HTTP call") {
      val sut = new DashboardReporter((_: String, _: String) => {
        HttpResponse("Bad request", 400, Map.empty)
      }, ciEnvironment)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      failedMessage shouldBe loggedAsError
      "Expected status code 201, but was 400. Body: 'Bad request'" shouldBe loggedAsError
      sentMessage should not be loggedAsInfo
    }
  }

  describe("resolveEnv") {
    class TestProvider() extends CiProvider {
      override def determineBranch(): Option[String] = Some("master")
      override def determineRepository(): Option[String] = Some("stryker-mutator/stryker4s")
      override def isPullRequest: Boolean = false
      override def determineApiKey(): Option[String] = Some("apiKey")
    }

    it("should prepend github.com/ to a provider") {
      val provider = new TestProvider()

      val env = DashboardReporter.tryResolveEnv(provider)

      env.value should equal(CiEnvironment("apiKey", s"github.com/${provider.determineRepository().value}", "master"))
    }

    it("should not create an environment in a PR") {
      val prProvider = new TestProvider() {
        override def isPullRequest: Boolean = true
      }

      val env = DashboardReporter.tryResolveEnv(prProvider)

      env shouldBe None
    }
  }
}
