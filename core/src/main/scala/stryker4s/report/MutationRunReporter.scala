package stryker4s.report
import mutationtesting._
import stryker4s.model.{Mutant, MutantRunResult}
import cats.effect.{Concurrent, ContextShift}
import scala.concurrent.Future

sealed trait MutationRunReporter

trait ProgressReporter extends MutationRunReporter {
  def reportMutationStart(mutant: Mutant): Future[Unit]

  def reportMutationComplete(result: MutantRunResult, totalMutants: Int): Future[Unit]
}

trait FinishedRunReporter extends MutationRunReporter {
  def reportRunFinished(runReport: FinishedRunReport): Future[Unit]
  def reportRunFinishedF[F[_]: Concurrent: ContextShift](runReport: FinishedRunReport): F[Unit]
}

final case class FinishedRunReport(report: MutationTestReport, metrics: MetricsResult) {
  @transient val timestamp: Long = System.currentTimeMillis()
}
