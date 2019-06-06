package stryker4s.report

import better.files.File
import org.mockito.captor.ArgCaptor
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import stryker4s.config.Config
import stryker4s.files.FileIO
import stryker4s.model.MutantRunResults
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

import scala.concurrent.duration._

class JsonReporterTest extends Stryker4sSuite with MockitoSugar with ArgumentMatchersSugar with LogMatchers {

  describe("reportJson") {
    it("should contain the report") {
      implicit val config: Config = Config()
      val mockFileIO = mock[FileIO]
      val sut = new JsonReporter(mockFileIO)
      val testFile = config.baseDir / "foo.bar"
      val mutationScore = 22.0
      val durationMinutes = 10
      val runResults = MutantRunResults(
        results = Seq(),
        mutationScore = mutationScore,
        duration = durationMinutes.minutes
      )

      sut.writeReportJsonTo(testFile, runResults)

      val expectedJs =
        s"""{"schemaVersion":"1","thresholds":{"high":80,"low":60},"files":{}}"""
      verify(mockFileIO).createAndWrite(testFile, expectedJs)
    }
  }

  describe("reportRunFinished") {
    implicit val config: Config = Config()
    val stryker4sReportFolderRegex = ".*target(/|\\\\)stryker4s-report-(\\d*)(/|\\\\)[a-z-]*\\.[a-z]*$"

    it("should write the report file to the report directory") {
      val mockFileIO = mock[FileIO]
      val sut = new JsonReporter(mockFileIO)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      val writtenFilesCaptor = ArgCaptor[File]

      verify(mockFileIO, times(1)).createAndWrite(writtenFilesCaptor, any[String])

      val paths = writtenFilesCaptor.values.map(_.pathAsString)
      all(paths) should fullyMatch regex stryker4sReportFolderRegex

      writtenFilesCaptor.values.map(_.name) should contain only "report.json"
    }

    it("should info log a message") {
      val mockFileIO = mock[FileIO]
      val sut = new JsonReporter(mockFileIO)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      "Written JSON report to " shouldBe loggedAsInfo
    }
  }
}
