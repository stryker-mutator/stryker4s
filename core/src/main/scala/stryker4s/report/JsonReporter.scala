package stryker4s.report

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.files.FileIO
import mutationtesting.MutationTestReport
import scala.concurrent.Future

class JsonReporter(fileIO: FileIO)(implicit config: Config) extends FinishedRunReporter with Logging {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def writeReportJsonTo(file: File, report: MutationTestReport): Future[Unit] = {
    import io.circe.syntax._
    import mutationtesting.MutationReportEncoder._
    val json = report.asJson.noSpaces
    fileIO.createAndWrite(file, json)
  }

  override def reportRunFinished(runReport: FinishedRunReport): Future[Unit] = {
    val targetLocation = config.baseDir / s"target/stryker4s-report-${runReport.timestamp}/"
    val resultLocation = targetLocation / "report.json"

    writeReportJsonTo(resultLocation, runReport.report)
      .map(_ => info(s"Written JSON report to $resultLocation"))

  }
}
