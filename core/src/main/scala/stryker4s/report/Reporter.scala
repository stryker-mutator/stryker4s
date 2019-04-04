package stryker4s.report

import grizzled.slf4j.Logging
import stryker4s.config.{Config, ConsoleReporterType, HtmlReporterType}
import stryker4s.files.DiskFileIO
import stryker4s.model.{Mutant, MutantRunResult, MutantRunResults}

import scala.util.{Failure, Try}

class Reporter(implicit config: Config) extends FinishedRunReporter with ProgressReporter with Logging {

  def reporters: Seq[MutationRunReporter] = {
    config.reporters collect {
      case ConsoleReporterType => new ConsoleReporter()
      case HtmlReporterType    => new HtmlReporter(DiskFileIO)
    }
  }

  private[this] val progressReporters = reporters collect { case r: ProgressReporter       => r }
  private[this] val finishedRunReporters = reporters collect { case r: FinishedRunReporter => r }

  override def reportMutationStart(mutant: Mutant): Unit =
    progressReporters.foreach(_.reportMutationStart(mutant))

  override def reportMutationComplete(result: MutantRunResult, totalMutants: Int): Unit =
    progressReporters.foreach(_.reportMutationComplete(result, totalMutants))

  override def reportRunFinished(runResults: MutantRunResults): Unit = {
    val reported = finishedRunReporters.map(reporter => Try(reporter.reportRunFinished(runResults)))
    val failed = reported.collect({ case f: Failure[Unit] => f })
    if (failed.nonEmpty) {
      warn(s"${failed.length} reporter(s) failed to report:")
      failed.map(_.exception).foreach(warn(_))
    }
  }

}
