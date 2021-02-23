package stryker4s.report

import java.nio.file.Path

import cats.Parallel
import cats.effect.IO
import mutationtesting._
import stryker4s.config.Config
import stryker4s.files.FileIO
import stryker4s.log.Logger

class HtmlReporter(fileIO: FileIO)(implicit log: Logger, p: Parallel[IO]) extends FinishedRunReporter {

  private val title = "Stryker4s report"
  private val mutationTestElementsName = "mutation-test-elements.js"
  private val htmlReportResource = s"/mutation-testing-elements/$mutationTestElementsName"
  private val reportFilename = "report.js"

  private val indexHtml: String =
    s"""<!DOCTYPE html>
       |<html lang="en">
       |<head>
       |  <meta charset="UTF-8">
       |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
       |  <script src="mutation-test-elements.js"></script>
       |</head>
       |<body>
       |  <mutation-test-report-app title-postfix="$title">
       |    Your browser doesn't support <a href="https://caniuse.com/#search=custom%20elements">custom elements</a>.
       |    Please use a latest version of an evergreen browser (Firefox, Chrome, Safari, Opera, etc).
       |  </mutation-test-report-app>
       |  <script>
       |    const app = document.getElementsByTagName('mutation-test-report-app').item(0)
       |    function updateTheme() {
       |      document.body.style.backgroundColor = app.theme === 'dark' ? '#222' : '#fff';
       |    }
       |    app.addEventListener('theme-changed', updateTheme);
       |    updateTheme();
       |  </script>
       |  <script src="$reportFilename"></script>
       |</body>
       |</html>""".stripMargin

  def writeMutationTestElementsJsTo(file: Path): IO[Unit] =
    fileIO.createAndWriteFromResource(file, htmlReportResource)

  def writeIndexHtmlTo(file: Path): IO[Unit] =
    fileIO.createAndWrite(file, indexHtml)

  def writeReportJsTo(file: Path, report: MutationTestResult[Config]): IO[Unit] = {
    import io.circe.syntax._
    import mutationtesting.circe._
    val json = report.asJson.noSpaces
    val reportContent = s"document.querySelector('mutation-test-report-app').report = $json"
    fileIO.createAndWrite(file, reportContent)
  }

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] = {
    val mutationTestElementsLocation = runReport.reportsLocation / mutationTestElementsName
    val indexLocation = runReport.reportsLocation / "index.html"
    val reportLocation = runReport.reportsLocation / reportFilename

    val reportsWriting = writeIndexHtmlTo(indexLocation.path) &>
      writeReportJsTo(reportLocation.path, runReport.report) &>
      writeMutationTestElementsJsTo(mutationTestElementsLocation.path)

    reportsWriting *>
      IO(log.info(s"Written HTML report to $indexLocation"))
  }
}
