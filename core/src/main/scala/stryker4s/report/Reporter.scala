package stryker4s.report

import grizzled.slf4j.Logging
import stryker4s.config._
import stryker4s.files.DiskFileIO
import stryker4s.model.{Mutant, MutantRunResult}
import stryker4s.report.dashboard.DashboardConfigProvider
import scala.util.{Failure, Try}
import cats.effect.{Concurrent, ContextShift}
import fs2.Stream
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import cats.effect.IO
import scala.concurrent.ExecutionContext

class Reporter(implicit config: Config) extends FinishedRunReporter with ProgressReporter with Logging {
  lazy val reporters: Iterable[MutationRunReporter] = config.reporters map {
    case Console => new ConsoleReporter()
    case Html    => new HtmlReporter(DiskFileIO)
    case Json    => new JsonReporter(DiskFileIO)
    case Dashboard =>
      implicit val contextShift = IO.contextShift(ExecutionContext.global)
      implicit val concurrent = IO.ioConcurrentEffect
      AsyncHttpClientCatsBackend().flatMap { _ => new DashboardReporter(new DashboardConfigProvider(sys.env)) }
      ???
  }

  private[this] val progressReporters = reporters collect { case r: ProgressReporter       => r }
  private[this] val finishedRunReporters = reporters collect { case r: FinishedRunReporter => r }

  override def reportMutationStart(mutant: Mutant): Unit =
    progressReporters.foreach(_.reportMutationStart(mutant))

  override def reportMutationComplete(result: MutantRunResult, totalMutants: Int): Unit =
    progressReporters.foreach(_.reportMutationComplete(result, totalMutants))

  override def reportRunFinished(runReport: FinishedRunReport): Unit = {
    val reported = finishedRunReporters.map(reporter => Try(reporter.reportRunFinished(runReport)))
    val failed = reported.collect({ case f: Failure[Unit] => f })
    if (failed.nonEmpty) {
      warn(s"${failed.size} reporter(s) failed to report:")
      failed.map(_.exception).foreach(warn(_))
    }
  }
  override def reportRunFinishedF[F[_]: Concurrent: ContextShift](runReport: FinishedRunReport): F[Unit] = {
    val reporterFs = finishedRunReporters
      .map(_.reportRunFinishedF(runReport))
      .map(fs2.Stream.eval(_))
      .toSeq
    Stream
      .emits(reporterFs)
      .handleErrorWith(e => { s"Reporter failed: ${e.getMessage}"; Stream.empty })
      .parJoinUnbounded
      .compile
      .drain
  }
}
