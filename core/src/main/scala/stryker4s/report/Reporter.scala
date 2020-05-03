package stryker4s.report

import grizzled.slf4j.Logging
import stryker4s.config._
import stryker4s.files.DiskFileIO
import stryker4s.model.{Mutant, MutantRunResult}
import stryker4s.report.dashboard.DashboardConfigProvider
import scala.util.{Failure, Try}
import sttp.client.HttpURLConnectionBackend

class Reporter(implicit config: Config) extends FinishedRunReporter with ProgressReporter with Logging {
  lazy val reporters: Iterable[MutationRunReporter] = config.reporters map {
    case Console => new ConsoleReporter()
    case Html    => new HtmlReporter(DiskFileIO)
    case Json    => new JsonReporter(DiskFileIO)
    case Dashboard =>
      implicit val backend = HttpURLConnectionBackend()
      new DashboardReporter(new DashboardConfigProvider(sys.env))
  }

  private[this] val progressReporters = reporters collect { case r: ProgressReporter => r }
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
}
