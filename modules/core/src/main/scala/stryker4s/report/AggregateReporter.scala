package stryker4s.report

import cats.effect.IO
import cats.syntax.all.*
import fs2.{CompositeFailure, Pipe, Stream}
import stryker4s.log.Logger

class AggregateReporter(reporters: List[Reporter])(implicit log: Logger) extends Reporter {

  override def mutantTested =
    reportAll(_.mutantTested)

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] =
    reporters
      .parTraverse { reporter =>
        IO.defer(reporter.onRunFinished(runReport))
          .as(none[Throwable])
          .handleErrorWith(e => logError(reporter, e).as(e.some))
      }
      .flatMap(errors => CompositeFailure.fromList(errors.flatten).traverse_(IO.raiseError))

  /** Broadcast to all reporters in parallel
    */
  private def reportAll[T](toReporterPipe: Reporter => Pipe[IO, T, Nothing]): Pipe[IO, T, Nothing] =
    if (reporters.isEmpty) _.drain
    else _.broadcastThrough(reporters.map(r => keepConsuming(r, toReporterPipe(r)))*)

  private def keepConsuming[T](reporter: Reporter, pipe: Pipe[IO, T, Nothing]): Pipe[IO, T, Nothing] =
    in => pipe(in).handleErrorWith(e => Stream.exec(logError(reporter, e))) ++ in.drain

  private def logError(reporter: Reporter, e: Throwable): IO[Unit] =
    IO(log.error(s"${reporter.getClass.getSimpleName} failed to report", e))
}
