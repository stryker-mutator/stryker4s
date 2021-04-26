package stryker4s.report

import cats.effect.IO
import fs2.{INothing, Pipe, Stream}
import stryker4s.log.Logger

class AggregateReporter(reporters: Seq[Reporter])(implicit log: Logger) extends Reporter {

  override def mutantPlaced = reportAll(_.mutantPlaced).andThen(_.attempt.drain)

  override def mutantTested =
    reportAll(_.mutantTested).andThen(_.attempt.drain)

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] =
    Stream
      .emit(()) // Single-event stream
      .through(reportAll(reporter => _.evalMap(_ => reporter.onRunFinished(runReport)).drain))
      .compile
      .drain

  /** Broadcast to all reporters in parallel
    */
  def reportAll[T](toReporterPipe: Reporter => Pipe[IO, T, INothing]): Pipe[IO, T, INothing] =
    _.broadcastThrough(reporters.map(toReporterPipe): _*).attempt
      .collect { case Left(f) => f }
      .evalMap { e =>
        IO(log.error(s"Reporter failed to report:", e)) *>
          IO.raiseError(e)
      }
}
