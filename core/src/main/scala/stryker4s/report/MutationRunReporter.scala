package stryker4s.report
import mutationtesting._
import stryker4s.model.{Mutant, MutantRunResult}
import cats.effect.{Concurrent, ContextShift}

trait MutationRunReporter

trait ProgressReporter extends MutationRunReporter {
  def reportMutationStart(mutant: Mutant): Unit

  def reportMutationComplete(result: MutantRunResult, totalMutants: Int): Unit
}

trait FinishedRunReporter extends MutationRunReporter {
  def reportRunFinished(runReport: FinishedRunReport): Unit
  def reportRunFinishedF[F[_]: Concurrent: ContextShift](runReport: FinishedRunReport): F[Unit]
}

case class FinishedRunReport(report: MutationTestReport, metrics: MetricsResult) {
  @transient val timestamp: Long = System.currentTimeMillis()
}
