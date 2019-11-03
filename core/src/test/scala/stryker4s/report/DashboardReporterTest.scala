package stryker4s.report

import mutationtesting.{Metrics, MutationTestReport}
import scalaj.http.HttpResponse
import stryker4s.config.Config
import stryker4s.http.WebIO
import stryker4s.report.dashboard.Providers.CiProvider
import stryker4s.report.model.StrykerDashboardReport
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoSuite, Stryker4sSuite}
import stryker4s.report.model.FullDashboardReport

class DashboardReporterTest extends Stryker4sSuite with MockitoSuite with LogMatchers {
  val ciEnvironment = CiEnvironment("someApiKey", "myRepo", "myBranch")
  describe("reportJson") {
    it("should contain the report") {
      implicit val config: Config = Config.default
      val mockWebIO = mock[WebIO]
      val sut = new DashboardReporter(mockWebIO, Some(ciEnvironment))
      val testUrl = "http://targetUrl.com"
      val report = MutationTestReport(thresholds = mutationtesting.Thresholds(80, 60), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      val convertToReport = sut.buildBody(report, metrics)
      sut.writeReportToDashboard(testUrl, convertToReport, ciEnvironment.apikey)

      val expectedJson = StrykerDashboardReport.toJson(convertToReport)
      convertToReport should equal(FullDashboardReport(report))
      verify(mockWebIO).putRequest(testUrl, expectedJson, Map("X-Api-Key" -> ciEnvironment.apikey))
    }
  }

  describe("reportRunFinished") {
    implicit val config: Config = Config.default
    val sentMessage = "Sent report to dashboard: https://dashboard.stryker-mutator.io"
    val failedMessage = "Failed to send report to dashboard."

    it("should info log a message on 200 success") {
      val sut = new DashboardReporter((_: String, _: String) => {
        HttpResponse("{\"href\":\"https://stryker-mutator.io\"}", 200, Map.empty)
      }, Some(ciEnvironment))
      val report = MutationTestReport(thresholds = mutationtesting.Thresholds(80, 60), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut.reportRunFinished(report, metrics)

      sentMessage shouldBe loggedAsInfo
      failedMessage should not be loggedAsError
      "Expected status code 200, but was " should not be loggedAsError
    }

    it("should error log on anything other than a 200") {
      val sut = new DashboardReporter((_: String, _: String) => {
        HttpResponse("null", 200, Map.empty)
      }, Some(ciEnvironment))
      val report = MutationTestReport(thresholds = mutationtesting.Thresholds(80, 60), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut.reportRunFinished(report, metrics)

      failedMessage shouldBe loggedAsError
      "Expected status code 200, but was 200. Body: 'null'" shouldBe loggedAsError
      sentMessage should not be loggedAsInfo
    }

    it("should error log on failed HTTP call") {
      val sut = new DashboardReporter((_: String, _: String) => {
        HttpResponse("Bad request", 400, Map.empty)
      }, Some(ciEnvironment))
      val report = MutationTestReport(thresholds = mutationtesting.Thresholds(80, 60), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut.reportRunFinished(report, metrics)

      failedMessage shouldBe loggedAsError
      "Expected status code 200, but was 400. Body: 'Bad request'" shouldBe loggedAsError
      sentMessage should not be loggedAsInfo
    }
  }

  describe("resolveEnv") {
    class TestProvider() extends CiProvider {
      override def determineProject(): Option[String] = Some("github.com/stryker-mutator/stryker4s")
      override def determineVersion(): Option[String] = Some("master")
      override def isPullRequest: Boolean = false
      override def determineApiKey(): Option[String] = Some("apiKey")
    }

    it("should prepend github.com/ to a provider") {
      val provider = new TestProvider()

      val env = DashboardReporter.tryResolveEnv(provider)

      env.value should equal(CiEnvironment("apiKey", s"github.com/${provider.determineProject().value}", "master"))
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
