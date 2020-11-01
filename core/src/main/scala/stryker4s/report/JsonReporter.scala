package stryker4s.report

import java.nio.file.Path

import cats.effect.IO
import stryker4s.log.Logger
import mutationtesting.MutationTestReport
import stryker4s.files.FileIO

class JsonReporter(fileIO: FileIO)(implicit log: Logger) extends FinishedRunReporter {

  def writeReportJsonTo(file: Path, report: MutationTestReport): IO[Unit] = {
    import io.circe.syntax._
    import mutationtesting.MutationReportEncoder._
    val json = report.asJson.noSpaces
    fileIO.createAndWrite(file, json)
  }

  override def reportRunFinished(runReport: FinishedRunReport): IO[Unit] = {
    val resultLocation = runReport.reportsLocation / "report.json"

    writeReportJsonTo(resultLocation.path, runReport.report) *>
      IO(log.info(s"Written JSON report to $resultLocation"))
  }
}
