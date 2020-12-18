package stryker4s.report

import cats.effect.{ContextShift, IO}
import cats.syntax.all._
import stryker4s.log.Logger

class AggregateReporter(reporters: Seq[MutationRunReporter])(implicit log: Logger, cs: ContextShift[IO])
    extends FinishedRunReporter
    with ProgressReporter {
  this: Reporter =>

  private lazy val progressReporters = reporters collect { case r: ProgressReporter => r }
  private lazy val finishedRunReporters = reporters collect { case r: FinishedRunReporter => r }

  override def reportMutationStart(event: StartMutationEvent): IO[Unit] =
    reportAll[ProgressReporter](
      progressReporters,
      _.reportMutationStart(event)
    )

  override def reportRunFinished(runReport: FinishedRunReport): IO[Unit] = {
    reportAll[FinishedRunReporter](
      finishedRunReporters,
      _.reportRunFinished(runReport)
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
          log.warn(s"${failed.size} reporter(s) failed to report:")
          failed.foreach(log.warn(_))
        }
        else IO.unit
      }
  }
}
