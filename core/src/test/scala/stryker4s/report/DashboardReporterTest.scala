package stryker4s.report

import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoSuite, Stryker4sSuite}
import sttp.client.testing.SttpBackendStub
import stryker4s.report.dashboard.DashboardConfigProvider
import stryker4s.report.model.DashboardConfig
import stryker4s.config.Full
import mutationtesting.MutationTestReport
import mutationtesting.Metrics
import sttp.client._
import sttp.model.{Header, HeaderNames, MediaType, Method}

class DashboardReporterTest extends Stryker4sSuite with MockitoSuite with LogMatchers {
  describe("buildRequest") {
    it("should compose the url") {
      implicit val backend = testBackend
      val mockDashConfig = mock[DashboardConfigProvider]
      val sut = new DashboardReporter(mockDashConfig)
      val dashConfig = DashboardConfig(
        "apiKeyHere",
        reportType = Full,
        baseUrl = "https://baseurl.com",
        project = "project/foo",
        version = "version/bar",
        module = None
      )
      val (report, metrics) = baseResults

      val request = sut.buildRequest(dashConfig, report, metrics)
      request.uri shouldBe uri"https://baseurl.com/api/reports/project/foo/version/bar"
      request.method shouldBe Method.PUT
      request.headers should contain(new Header("X-Api-Key", "apiKeyHere"))
      request.headers should contain(new Header(HeaderNames.ContentType, MediaType.ApplicationJson.toString()))
    }
  }

  def testBackend = SttpBackendStub.synchronous

  def baseResults = {
    val report = MutationTestReport(thresholds = mutationtesting.Thresholds(80, 60), files = Map.empty)
    val metrics = Metrics.calculateMetrics(report)
    (report, metrics)
  }
}
