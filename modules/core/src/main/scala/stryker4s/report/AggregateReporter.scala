package stryker4s.report

import cats.effect.IO
import cats.syntax.all.*
import fs2.Pipe
import stryker4s.log.Logger

class AggregateReporter(reporters: List[Reporter])(implicit log: Logger) extends Reporter {

  override def mutantTested =
    reportAll(_.mutantTested)

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] =
    reporters.parTraverse_(_.onRunFinished(runReport))

  /** Broadcast to all reporters in parallel
    */
  private def reportAll[T](toReporterPipe: Reporter => Pipe[IO, T, Nothing]): Pipe[IO, T, Nothing] = {
    val pipes = reporters.map(toReporterPipe)
    if (pipes.isEmpty) _.drain
    else
      _.broadcastThrough(reporters.map(toReporterPipe)*).attempt
        .collect { case Left(f) => f }
        .evalMap { e =>
          IO(log.error(s"Reporter failed to report", e))
        }
        .drain
  }
}
