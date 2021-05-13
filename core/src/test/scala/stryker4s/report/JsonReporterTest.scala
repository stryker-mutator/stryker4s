package stryker4s.report

import java.nio.file.Path

import scala.concurrent.duration._

import better.files.File
import mutationtesting.{Metrics, MutationTestResult, Thresholds}
import org.mockito.captor.ArgCaptor
import stryker4s.files.FileIO
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite}

class JsonReporterTest extends Stryker4sIOSuite with MockitoIOSuite with LogMatchers {
  describe("reportJson") {
    it("should contain the report") {
      val mockFileIO = mock[FileIO]
      whenF(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(())
      val sut = new JsonReporter(mockFileIO)
      val testFile = File("foo.bar").path
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)

      sut
        .writeReportJsonTo(testFile, report)
        .map { _ =>
          verify(mockFileIO).createAndWrite(eqTo(testFile), any[String])
        }
        .assertNoException
    }
  }

  describe("onRunFinished") {
    val reportLocation = File("target") / "stryker4s-report"
    it("should write the report file to the report directory") {
      val mockFileIO = mock[FileIO]
      whenF(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(())
      val sut = new JsonReporter(mockFileIO)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 10.seconds, reportLocation))
        .asserting { _ =>
          val writtenFilesCaptor = ArgCaptor[Path]
          verify(mockFileIO, times(1)).createAndWrite(writtenFilesCaptor, any[String])
          val path = writtenFilesCaptor.value.toString()
          path should endWith((reportLocation / "report.json").toString)
        }
    }

    it("should info log a message") {
      val mockFileIO = mock[FileIO]
      whenF(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(())
      val sut = new JsonReporter(mockFileIO)
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)
      val captor = ArgCaptor[Path]
      sut
        .onRunFinished(FinishedRunEvent(report, metrics, 10.seconds, reportLocation))
        .asserting { _ =>
          verify(mockFileIO).createAndWrite(captor.capture, any[String])
          s"Written JSON report to ${(reportLocation / "report.json").toString}" shouldBe loggedAsInfo
        }
    }
  }
}
