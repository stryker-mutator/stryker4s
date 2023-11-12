package stryker4s.report

import fs2.io.file.Path
import mutationtesting.{Metrics, MutationTestResult, Thresholds}
import org.mockito.captor.ArgCaptor
import stryker4s.files.FileIO
import stryker4s.testkit.{LogMatchers, MockitoSuite, Stryker4sIOSuite}

import scala.concurrent.duration.*

class JsonReporterTest extends Stryker4sIOSuite with MockitoSuite with LogMatchers {
  describe("reportJson") {

    test("should contain the report") {
      val mockFileIO = mock[FileIO]
      whenF(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(())
      val sut = new JsonReporter(mockFileIO)
      val testFile = Path("foo.bar")
      val report = MutationTestResult(thresholds = Thresholds(100, 0), files = Map.empty)

      sut
        .writeReportJsonTo(testFile, report)
        .map { _ =>
          verify(mockFileIO).createAndWrite(eqTo(testFile), any[String])
        }
        .void
        .assert
    }
  }

  describe("onRunFinished") {
    val reportLocation = Path("target") / "stryker4s-report"
    test("should write the report file to the report directory") {
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
          val path = writtenFilesCaptor.value.toString
          assert(path.endsWith((reportLocation / "report.json").toString))
        }
    }

    test("should info log a message") {
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
          assertLoggedInfo(s"Written JSON report to ${captor.value.toString}")
        }
    }
  }
}
