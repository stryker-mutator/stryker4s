package stryker4s.report

import cats.effect.IO
import cats.syntax.parallel._
import fs2.{INothing, Pipe}
import stryker4s.log.Logger

class AggregateReporter(reporters: List[Reporter])(implicit log: Logger) extends Reporter {

  override def mutantTested =
    reportAll(_.mutantTested)

  override def onRunFinished(runReport: FinishedRunEvent): IO[Unit] =
    reporters.parTraverse_(_.onRunFinished(runReport))

  /** Broadcast to all reporters in parallel
    */
  private def reportAll[T](toReporterPipe: Reporter => Pipe[IO, T, INothing]): Pipe[IO, T, INothing] = {
    val pipes = reporters.map(toReporterPipe)
    if (pipes.isEmpty) _.drain
    else
      _.broadcastThrough(reporters.map(toReporterPipe): _*).attempt
        .collect { case Left(f) => f }
        .evalMap { e =>
          IO(log.error(s"Reporter failed to report", e))
        }
        .drain
  }
}
