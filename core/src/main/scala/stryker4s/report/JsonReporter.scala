package stryker4s.report

import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.files.FileIO
import mutationtesting.MutationTestReport
import cats.effect.IO
import java.nio.file.Path

class JsonReporter(fileIO: FileIO)(implicit config: Config) extends FinishedRunReporter with Logging {

  def writeReportJsonTo(file: Path, report: MutationTestReport): IO[Unit] = {
    import io.circe.syntax._
    import mutationtesting.MutationReportEncoder._
    val json = report.asJson.noSpaces
    fileIO.createAndWrite(file, json)
  }

  override def reportRunFinished(runReport: FinishedRunReport): IO[Unit] = {
    val targetLocation = config.baseDir / s"target/stryker4s-report-${runReport.timestamp}/"
    val resultLocation = targetLocation / "report.json"

    writeReportJsonTo(resultLocation.path, runReport.report) *>
      IO(info(s"Written JSON report to $resultLocation"))
  }
}
