package stryker4s.report

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.files.FileIO
import stryker4s.model.MutantRunResults
import stryker4s.report.mapper.MutantRunResultMapper
import stryker4s.report.model.MutationTestReport

class JsonReporter(fileIO: FileIO)(implicit config: Config)
    extends FinishedRunReporter
    with MutantRunResultMapper
    with Logging {

  def buildScoreResult(report: MutantRunResults): MutationTestReport = {
    toReport(report)
  }

  def writeReportJsonTo(file: File, report: MutantRunResults): Unit = {
    val content = buildScoreResult(report)
    fileIO.createAndWrite(file, content.toJson)
  }

  override def reportRunFinished(runResults: MutantRunResults): Unit = {
    val targetLocation = config.baseDir / s"target/stryker4s-report-${runResults.timestamp}"
    val resultLocation = targetLocation / "report.json"

    writeReportJsonTo(resultLocation, runResults)

    info(s"Written JSON report to $resultLocation")
  }
}
