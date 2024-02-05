package stryker4s.report

import cats.effect.IO
import fs2.io.file.Path
import mutationtesting.MutationTestResult
import stryker4s.config.Config
import stryker4s.config.codec.CirceConfigEncoder
import stryker4s.files.FileIO
import stryker4s.log.Logger

class JsonReporter(fileIO: FileIO)(implicit log: Logger) extends Reporter with CirceConfigEncoder {

  def writeReportJsonTo(file: Path, report: MutationTestResult[Config]): IO[Unit] = {
    import io.circe.syntax.*
    import mutationtesting.circe.*
    val json = report.asJson.noSpaces
    fileIO.createAndWrite(file, json)
  }

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] = {
    val resultLocation = runReport.reportsLocation / "report.json"

    writeReportJsonTo(resultLocation, runReport.report) *>
      IO(log.info(s"Written JSON report to $resultLocation"))
  }
}
