package stryker4s.report

import fs2.io.file.Path
import mutationtesting.{Metrics, MutationTestResult, Thresholds}
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.stubs.FileIOStub

import scala.concurrent.duration.*

class JsonReporterTest extends Stryker4sIOSuite with LogMatchers {
  describe("reportJson") {

    test("should contain the report") {
      val fileIOStub = FileIOStub()
      val sut = new JsonReporter(fileIOStub)
      val testFile = Path("foo.bar")
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)

      sut
        .writeReportJsonTo(testFile, report) >>
        fileIOStub.createAndWriteCalls.asserting { calls =>
          assertEquals(calls.loneElement._1, testFile)
          assert(calls.loneElement._2.nonEmpty)
        }
    }
  }

  describe("onRunFinished") {
    val reportLocation = Path("target") / "stryker4s-report"
    test("should write the report file to the report directory") {
      val fileIOStub = FileIOStub()
      val sut = new JsonReporter(fileIOStub)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 10.seconds, reportLocation)) >>
        fileIOStub.createAndWriteCalls.asserting { calls =>
          assertEquals(calls.loneElement._1, reportLocation / "report.json")
        }
    }

    test("should info log a message") {
      val fileIOStub = FileIOStub()
      val sut = new JsonReporter(fileIOStub)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 10.seconds, reportLocation)) >>
        fileIOStub.createAndWriteCalls.asserting { calls =>
          assertLoggedInfo(s"Written JSON report to ${calls.loneElement._1.toString}")
        }
    }
  }
}
