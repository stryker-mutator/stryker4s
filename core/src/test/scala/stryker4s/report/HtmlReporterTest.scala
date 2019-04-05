package stryker4s.report
import better.files.File
import org.mockito.captor.ArgCaptor
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import stryker4s.config.Config
import stryker4s.files.{DiskFileIO, FileIO}
import stryker4s.model.MutantRunResults
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

import scala.concurrent.duration._
import scala.io.Source

class HtmlReporterTest extends Stryker4sSuite with MockitoSugar with ArgumentMatchersSugar with LogMatchers {

  describe("indexHtml") {
    it("should contain title") {
      implicit val config: Config = Config()
      val mockFileIO = mock[FileIO]
      val resourceLocation = "mutation-testing-elements/mutation-test-elements.js"
      when(mockFileIO.readResource(resourceLocation)) thenReturn Source.fromString("console.log('hello');")
      val sut = new HtmlReporter(mockFileIO)

      val result = sut.indexHtml()

      val expected = s"""<!DOCTYPE html>
                        |<html>
                        |<body>
                        |  <mutation-test-report-app title-postfix="Stryker4s report">
                        |    Your browser doesn't support <a href="https://caniuse.com/#search=custom%20elements">custom elements</a>.
                        |    Please use a latest version of an evergreen browser (Firefox, Chrome, Safari, Opera, etc).
                        |  </mutation-test-report-app>
                        |  <script src="report.js"></script>
                        |  <script>
                        |    console.log('hello');
                        |  </script>
                        |</body>
                        |</html>""".stripMargin
      result.mkString should equal(expected)
    }
  }

  describe("reportJs") {
    it("should contain the report") {
      implicit val config: Config = Config()
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)

      val result = sut.reportJs("""{ 'foo': 'bar' }""")

      val expected = s"""document.querySelector('mutation-test-report-app').report = { 'foo': 'bar' }"""
      result.mkString should equal(expected)

    }
  }

  describe("mutation-test-elements") {
    it("should find the resource") {
      implicit val config: Config = Config()
      val fileIO = DiskFileIO

      val sut = new HtmlReporter(fileIO)

      val result = sut.indexHtml()
      result.mkString.length should be > 50
    }
  }

  describe("reportRunFinished") {
    it("should write the report files to the report directory") {
      implicit val config: Config = Config()
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      val indexCaptor = ArgCaptor[File]
      val reportCaptor = ArgCaptor[File]
      verify(mockFileIO).createAndWrite(indexCaptor, any[Iterator[Char]])
      verify(mockFileIO).createAndWrite(reportCaptor, any[String])
      val paths = List(indexCaptor.value, reportCaptor.value).map(_.pathAsString)

      // ends with target/stryker4s-report-$TIMESTAMP/filename.extension
      all(paths) should fullyMatch regex ".*target(/|\\\\)stryker4s-report-(\\d*)(/|\\\\)[a-z]*\\.[a-z]*$"
      indexCaptor.value.name should be("index.html")
      reportCaptor.value.name should be("report.js")
    }

    it("should debug log a message") {
      implicit val config: Config = Config()
      val mockFileIO = mock[FileIO]
      val sut = new HtmlReporter(mockFileIO)
      val runResults = MutantRunResults(Nil, 50.0, 30.seconds)

      sut.reportRunFinished(runResults)

      "Written HTML report to " shouldBe loggedAsDebug
    }
  }
}
