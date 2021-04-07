package stryker4s.report

import scala.util.control.NonFatal

import cats.effect.util.CompositeException
import cats.effect.IO
import cats.syntax.all._
import stryker4s.log.Logger

class AggregateReporter(reporters: Seq[MutationRunReporter])(implicit log: Logger, cs: ContextShift[IO])
    extends FinishedRunReporter
    with ProgressReporter {
  this: Reporter =>

  private lazy val progressReporters = reporters collect { case r: ProgressReporter => r }
  private lazy val finishedRunReporters = reporters collect { case r: FinishedRunReporter => r }

  override def onMutationStart(event: StartMutationEvent): IO[Unit] =
    reportAll[ProgressReporter](
      progressReporters,
      _.onMutationStart(event)
    )
      .onError { case NonFatal(e) =>
        IO(log.error(e))
      }
      .attempt
      .void

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] = {
    reportAll[FinishedRunReporter](
      finishedRunReporters,
      _.onRunFinished(runReport)
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
      .flatMap {
        // No reporter failed
        case Nil =>
          IO.unit
        // 1 reporter failed
        case e :: Nil =>
          IO(log.error("1 reporter failed to report:")) *>
            IO.raiseError(e)
        // Multiple reporters failed
        case firstException :: secondException :: rest =>
          val e = CompositeException(firstException, secondException, rest)
          IO(log.error(s"${rest.length + 2} reporters failed to report:")) *>
            IO.raiseError(e)
      }
  }
}
