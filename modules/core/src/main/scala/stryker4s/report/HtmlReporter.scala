package stryker4s.report

import cats.Monad
import cats.effect.IO
import fs2.io.file.Path
import mutationtesting.*
import stryker4s.config.Config
import stryker4s.config.codec.CirceConfigEncoder
import stryker4s.files.FileIO
import stryker4s.log.Logger

class HtmlReporter(fileIO: FileIO)(implicit log: Logger) extends Reporter with CirceConfigEncoder {

  private val title = "Stryker4s report"
  private val mutationTestElementsName = "mutation-test-elements.js"
  private val htmlReportResource = s"/elements/$mutationTestElementsName"
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
       |    const app = document.getElementsByTagName('mutation-test-report-app').item(0);
       |    function updateTheme() {
       |      document.body.style.backgroundColor = app.themeBackgroundColor;
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
    import io.circe.syntax.*
    import mutationtesting.circe.*
    val json = report.asJson.noSpaces
    val reportContent = s"document.querySelector('mutation-test-report-app').report = $json"
    fileIO.createAndWrite(file, reportContent)
  }

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] = {
    val mutationTestElementsLocation = runReport.reportsLocation / mutationTestElementsName
    val indexLocation = runReport.reportsLocation / "index.html"
    val reportLocation = runReport.reportsLocation / reportFilename

    val reportsWriting = writeIndexHtmlTo(indexLocation) &>
      writeReportJsTo(reportLocation, runReport.report) &>
      writeMutationTestElementsJsTo(mutationTestElementsLocation)

    def desktopSupportedAndFileExists: IO[Boolean] = IO(
      Desktop.isDesktopSupported && indexLocation.toNioPath.toFile.exists()
    )

    def openFile: IO[Unit] = IO(Desktop.getDesktop.open(indexLocation.toNioPath.toFile))

    val openFileIO = Monad[IO].ifElseM(
      desktopSupportedAndFileExists -> openFile
    )(
      IO(log.warn("Desktop operations not supported or file does not exist."))
    )

    for {
      _ <- reportsWriting
      _ <- IO(log.info(s"Written HTML report to $indexLocation"))
      _ <- if (openReportAutomatically) openFileIO else IO.unit
    } yield ()

  }
}
