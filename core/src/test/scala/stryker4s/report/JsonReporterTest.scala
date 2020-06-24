package stryker4s.report

import better.files.File
import mutationtesting.{Metrics, MutationTestReport, Thresholds}
import org.mockito.captor.ArgCaptor
import stryker4s.config.Config
import stryker4s.files.FileIO
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.{AsyncStryker4sSuite, MockitoSuite}

class JsonReporterTest extends AsyncStryker4sSuite with MockitoSuite with LogMatchers {
  describe("reportJson") {
    it("should contain the report") {
      implicit val config: Config = Config.default
      val mockFileIO = mock[FileIO]
      val sut = new JsonReporter(mockFileIO)
      val testFile = config.baseDir / "foo.bar"
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)

      (for {
        _ <- sut.writeReportJsonTo(testFile, report)

        _ <- verify(mockFileIO).createAndWrite(eqTo(testFile), any[String])
      } yield succeed).unsafeToFuture()
    }
  }

  describe("reportRunFinished") {
    implicit val config: Config = Config.default
    val stryker4sReportFolderRegex = ".*target(/|\\\\)stryker4s-report-(\\d*)(/|\\\\)[a-z-]*\\.[a-z]*$"

    it("should write the report file to the report directory") {
      val mockFileIO = mock[FileIO]
      val sut = new JsonReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      (for {
        _ <- sut.reportRunFinished(FinishedRunReport(report, metrics))

        writtenFilesCaptor = ArgCaptor[File]
        _ <- verify(mockFileIO, times(1)).createAndWrite(writtenFilesCaptor, any[String])
        paths = writtenFilesCaptor.values.map(_.pathAsString)
        _ = all(paths) should fullyMatch regex stryker4sReportFolderRegex
        assertion = writtenFilesCaptor.values.map(_.name) should contain only "report.json"
      } yield assertion).unsafeToFuture()
    }

    it("should info log a message") {
      val mockFileIO = mock[FileIO]
      val sut = new JsonReporter(mockFileIO)
      val report = MutationTestReport(thresholds = Thresholds(100, 0), files = Map.empty)
      val metrics = Metrics.calculateMetrics(report)

      (for {
        _ <- sut.reportRunFinished(FinishedRunReport(report, metrics))

        captor = ArgCaptor[File]
        _ <- verify(mockFileIO).createAndWrite(captor.capture, any[String])
        assertion = s"Written JSON report to ${captor.value}" shouldBe loggedAsInfo
      } yield assertion).unsafeToFuture()
    }
  }
}
