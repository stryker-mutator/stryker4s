package stryker4s.report
import mutationtesting._
import stryker4s.model.{Mutant, MutantRunResult}

trait MutationRunReporter

trait ProgressReporter extends MutationRunReporter {
  def reportMutationStart(mutant: Mutant): Unit

  def reportMutationComplete(result: MutantRunResult, totalMutants: Int): Unit
}

trait FinishedRunReporter extends MutationRunReporter {
  def reportRunFinished(runReport: FinishedRunReport): Unit
}

case class FinishedRunReport(report: MutationTestReport, metrics: MetricsResult)
