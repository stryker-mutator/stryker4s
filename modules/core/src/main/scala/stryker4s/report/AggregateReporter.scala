package stryker4s.report

import cats.effect.IO
import cats.syntax.all.*
import fs2.{Pipe, Stream}
import stryker4s.log.Logger

class AggregateReporter(reporters: List[Reporter])(implicit log: Logger) extends Reporter {

  override def mutantTested =
    reportAll(_.mutantTested)

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] =
    reporters.parTraverse_(_.onRunFinished(runReport))

  /** Broadcast to all reporters in parallel
    */
  private def reportAll[T](toReporterPipe: Reporter => Pipe[IO, T, Nothing]): Pipe[IO, T, Nothing] = {
    def logFailure(e: Throwable): Stream[IO, Nothing] =
      Stream.exec(IO(log.error("Reporter failed to report", e)))

    val pipes = reporters.map { reporter => (in: Stream[IO, T]) =>
      in.through(toReporterPipe(reporter)).handleErrorWith(logFailure)
    }
    if (pipes.isEmpty) _.drain
    else
      _.broadcastThrough(pipes*).handleErrorWith(logFailure)
  }
}
