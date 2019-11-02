package stryker4s.report

import better.files.File
import grizzled.slf4j.Logging
import mutationtesting.{MetricsResult, MutationTestReport}
import stryker4s.config.Config
import stryker4s.files.FileIO
import stryker4s.report.mapper.MutantRunResultMapper

class JsonReporter(fileIO: FileIO)(implicit config: Config)
    extends FinishedRunReporter
    with MutantRunResultMapper
    with Logging {
  def writeReportJsonTo(file: File, report: MutationTestReport): Unit = {
    import io.circe.syntax._
    import mutationtesting.MutationReportEncoder._
    val json = report.asJson.noSpaces
    fileIO.createAndWrite(file, json)
  }

  override def reportRunFinished(report: MutationTestReport, metrics: MetricsResult): Unit = {
    val targetLocation = config.baseDir / s"target/stryker4s-report"
    val resultLocation = targetLocation / "report.json"

    writeReportJsonTo(resultLocation, report)

    info(s"Written JSON report to $resultLocation")
  }
}
