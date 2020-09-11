package stryker4s.report

import java.nio.file.Path

import cats.effect.IO
import grizzled.slf4j.Logging
import mutationtesting.MutationTestReport
import stryker4s.files.FileIO

class JsonReporter(fileIO: FileIO) extends FinishedRunReporter with Logging {

  def writeReportJsonTo(file: Path, report: MutationTestReport): IO[Unit] = {
    import io.circe.syntax._
    import mutationtesting.MutationReportEncoder._
    val json = report.asJson.noSpaces
    fileIO.createAndWrite(file, json)
  }

  override def reportRunFinished(runReport: FinishedRunReport): IO[Unit] = {
    val resultLocation = runReport.reportsLocation / "report.json"

    writeReportJsonTo(resultLocation.path, runReport.report) *>
      IO(info(s"Written JSON report to $resultLocation"))
  }
}
