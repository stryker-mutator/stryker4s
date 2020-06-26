package stryker4s.report

import mutationtesting.{Metrics, MutationTestReport, Thresholds}
import org.mockito.captor.ArgCaptor
import stryker4s.config.Config
import stryker4s.files.FileIO
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{MockitoSuite, Stryker4sSuite}
import java.nio.file.Path
import cats.effect.IO

class JsonReporterTest extends Stryker4sSuite with MockitoSuite with LogMatchers {
  describe("reportJson") {
    it("should contain the report") {
      implicit val config: Config = Config.default
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new JsonReporter(mockFileIO)
      val testFile = (config.baseDir / "foo.bar").path
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)

      sut
        .writeReportJsonTo(testFile, report)
        .unsafeRunSync()
      verify(mockFileIO).createAndWrite(eqTo(testFile), any[String])
    }
  }

  describe("reportRunFinished") {
    implicit val config: Config = Config.default
    val stryker4sReportFolderRegex = ".*target(/|\\\\)stryker4s-report-(\\d*)(/|\\\\)[a-z-]*\\.[a-z]*$"

    it("should write the report file to the report directory") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new JsonReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      sut
        .reportRunFinished(FinishedRunReport(report, metrics))
        .unsafeRunSync()

      val writtenFilesCaptor = ArgCaptor[Path]
      verify(mockFileIO, times(1)).createAndWrite(writtenFilesCaptor, any[String])
      val paths = writtenFilesCaptor.values.map(_.toString())
      all(paths) should fullyMatch regex stryker4sReportFolderRegex
      writtenFilesCaptor.values.map(_.getFileName().toString()) should contain only "report.json"
    }

    it("should info log a message") {
      val mockFileIO = mock[FileIO]
      when(mockFileIO.createAndWrite(any[Path], any[String])).thenReturn(IO.unit)
      val sut = new JsonReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      val captor = ArgCaptor[Path]
      sut
        .reportRunFinished(FinishedRunReport(report, metrics))
        .unsafeRunSync()

      verify(mockFileIO).createAndWrite(captor.capture, any[String])
      s"Written JSON report to ${captor.value}" shouldBe loggedAsInfo
    }
  }
}
