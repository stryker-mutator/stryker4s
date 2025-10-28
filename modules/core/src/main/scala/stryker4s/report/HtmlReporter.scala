package stryker4s.report

import cats.effect.IO
import cats.syntax.applicative.*
import fs2.{text, Stream}
import mutationtesting.*
import stryker4s.config.Config
import stryker4s.config.codec.CirceConfigEncoder
import stryker4s.files.{DesktopIO, FileIO}
import stryker4s.log.Logger

class HtmlReporter(fileIO: FileIO, desktopIO: DesktopIO)(implicit config: Config, log: Logger)
    extends Reporter
    with CirceConfigEncoder {

  private val title = "Stryker4s report"

  private val indexHtmlStart =
    s"""|<!DOCTYPE html>
        |<html lang="en">
        |  <head>
        |    <meta charset="UTF-8">
        |    <meta name="viewport" content="width=device-width, initial-scale=1.0">
        |    <script>
        |""".stripMargin
  // scriptContent
  private val indexHtmlMiddle =
    s"""|    </script>
        |  </head>
        |  <body>
        |    <mutation-test-report-app title-postfix="$title">
        |      Your browser doesn't support <a href="https://caniuse.com/#search=custom%20elements">custom elements</a>.
        |      Please use a latest version of an evergreen browser (Firefox, Chrome, Safari, Opera, etc).
        |    </mutation-test-report-app>
        |    <script>
        |      const app = document.querySelector('mutation-test-report-app');
        |""".stripMargin
  // app.report = reportJson
  private val indexHtmlEnd =
    s"""|      function updateTheme() {
        |        document.body.style.backgroundColor = app.themeBackgroundColor;
        |      }
        |      app.addEventListener('theme-changed', updateTheme);
        |      updateTheme();
        |    </script>
        |  </body>
        |</html>
        |""".stripMargin

  def reportAsJsonStr(report: MutationTestResult[Config]): String = {
    import io.circe.syntax.*
    import mutationtesting.circe.*
    // Escapes the HTML tags inside strings in a JSON input by breaking them apart.
    val json = report.asJson.noSpaces.replaceAll("<", "<\"+\"")
    s"      app.report = $json;\n"
  }

  def createHtmlReportStream(report: MutationTestResult[Config]): Stream[IO, Byte] = {
    val mutationTestElementsJsResource = "/elements/mutation-test-elements.js"

    def emitStr(str: String) = text.utf8.encode(Stream.emit(str))

    emitStr(indexHtmlStart) ++
      fileIO.resourceAsStream(mutationTestElementsJsResource) ++
      emitStr(indexHtmlMiddle) ++
      emitStr(reportAsJsonStr(report)) ++
      emitStr(indexHtmlEnd)
  }

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] = {
    val indexLocation = runReport.reportsLocation / "index.html"
    for {
      _ <- fileIO.createAndWrite(indexLocation, createHtmlReportStream(runReport.report))
      _ <- IO(log.info(s"Written HTML report to $indexLocation"))
      _ <- desktopIO
        .attemptOpen(indexLocation)
        .handleErrorWith(e => IO(log.error("Error opening report in browser", e)))
        .whenA(config.openReport)
    } yield ()
  }
}
