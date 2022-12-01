package stryker4jvm.reporting

import cats.effect.IO
import cats.syntax.parallel.*
import fs2.Pipe
import stryker4jvm.config.Config
import stryker4jvm.core.logging.Logger
import stryker4jvm.core.model.AST
import stryker4jvm.core.reporting.Reporter
import stryker4jvm.core.reporting.events.FinishedRunEvent

class AggregateReporter(reporters: List[Reporter[Config]])(implicit log: Logger) extends Reporter {

  override def mutantTested =
    reportAll(_.mutantTested)

  override def onRunFinished(runReport: FinishedRunEvent[Config]): IO[Unit] =
    reporters.parTraverse_(_.onRunFinished(runReport))

  /** Broadcast to all reporters in parallel
    */
  private def reportAll[T](toReporterPipe: Reporter[Config] => Pipe[IO, T, Nothing]): Pipe[IO, T, Nothing] = {
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
