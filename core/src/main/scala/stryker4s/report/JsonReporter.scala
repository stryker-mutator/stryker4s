package stryker4s.report

import java.nio.file.Path

import cats.effect.IO
import mutationtesting.MutationTestResult
import stryker4s.files.FileIO
import stryker4s.log.Logger

class JsonReporter(fileIO: FileIO)(implicit log: Logger) extends FinishedRunReporter {

  def writeReportJsonTo(file: Path, report: MutationTestResult): IO[Unit] = {
    import io.circe.syntax._
    import mutationtesting.circe._
    val json = report.asJson.noSpaces
    fileIO.createAndWrite(file, json)
  }

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] = {
    val resultLocation = runReport.reportsLocation / "report.json"

    writeReportJsonTo(resultLocation.path, runReport.report) *>
      IO(log.info(s"Written JSON report to $resultLocation"))
  }
}
