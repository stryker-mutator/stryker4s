package stryker4s.report
import mutationtesting._
import stryker4s.model.{Mutant, MutantRunResult}
import cats.effect.IO

sealed trait MutationRunReporter

trait ProgressReporter extends MutationRunReporter {
  def reportMutationStart(mutant: Mutant): IO[Unit]

  def reportMutationComplete(result: MutantRunResult, totalMutants: Int): IO[Unit]
}

trait FinishedRunReporter extends MutationRunReporter {
  def reportRunFinished(runReport: FinishedRunReport): IO[Unit]
}

final case class FinishedRunReport(report: MutationTestReport, metrics: MetricsResult) {
  @transient val timestamp: Long = System.currentTimeMillis()
}
