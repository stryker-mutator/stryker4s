package stryker4s.report

import java.nio.file.Path

import scala.concurrent.duration._

import better.files.File
import cats.effect.IO
import mutationtesting.{Metrics, MutationTestReport, Thresholds}
import org.mockito.captor.ArgCaptor
import stryker4s.files.FileIO
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite}

class JsonReporterTest extends Stryker4sIOSuite with MockitoIOSuite with LogMatchers {
  describe("reportJson") {
    it("should contain the report") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new JsonReporter(mockFileIO)
      val testFile = File("foo.bar").path
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)

      sut
        .writeReportJsonTo(testFile, report)
        .map { _ =>
          verify(mockFileIO).createAndWrite(eqTo(testFile), any[String])
        }
        .assertNoException
    }
  }

  describe("reportRunFinished") {
    it("should write the report file to the report directory") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new JsonReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .reportRunFinished(FinishedRunReport(report, metrics, 10.seconds, File("target/stryker4s-report/")))
        .asserting { _ =>
          val writtenFilesCaptor = ArgCaptor[Path]
          verify(mockFileIO, times(1)).createAndWrite(writtenFilesCaptor, any[String])
          val path = writtenFilesCaptor.value.toString()
          path should endWith("/target/stryker4s-report/report.json")
        }
    }

    it("should info log a message") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new JsonReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)
      val reportFile = File("target/stryker4s-report/")
      val captor = ArgCaptor[Path]
      sut
        .reportRunFinished(FinishedRunReport(report, metrics, 10.seconds, reportFile))
        .asserting { _ =>
          verify(mockFileIO).createAndWrite(captor.capture, any[String])
          s"Written JSON report to ${reportFile.toString}/report.json" shouldBe loggedAsInfo
        }
    }
  }
}
