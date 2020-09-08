package stryker4s.report

import cats.effect.{ContextShift, IO}
import cats.syntax.all._
import grizzled.slf4j.Logging
import stryker4s.model.{Mutant, MutantRunResult}

class AggregateReporter(reporters: Seq[MutationRunReporter])(implicit cs: ContextShift[IO])
    extends FinishedRunReporter
    with ProgressReporter
    with Logging {
  this: Reporter =>

  private lazy val progressReporters = reporters collect { case r: ProgressReporter => r }
  private lazy val finishedRunReporters = reporters collect { case r: FinishedRunReporter => r }

  override def reportMutationStart(mutant: Mutant): IO[Unit] =
    reportAll[ProgressReporter](
      progressReporters,
      _.reportMutationStart(mutant)
    )

  override def reportMutationComplete(result: MutantRunResult, totalMutants: Int): IO[Unit] =
    reportAll[ProgressReporter](
      progressReporters,
      _.reportMutationComplete(result, totalMutants)
    )

  override def reportRunFinished(runReport: FinishedRunReport): IO[Unit] = {
    reportAll[FinishedRunReporter](
      finishedRunReporters,
      reporter => reporter.reportRunFinished(runReport)
    )
  }

  /** Calls all @param reporters with the given @param reportF function, logging any that failed
    *
    * @param reporters
    * @param reportF
    */
  private def reportAll[T](reporters: Iterable[T], reportF: T => IO[Unit]): IO[Unit] = {
    reporters.toList
      .parTraverse { reporter =>
        reportF(reporter).attempt
      }
      .map { _ collect { case Left(f) => f } }
      .flatMap { failed =>
        if (failed.nonEmpty) IO {
          warn(s"${failed.size} reporter(s) failed to report:")
          failed.foreach(warn(_))
        }
        else IO.unit
      }
  }
}
