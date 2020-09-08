package stryker4s.run

import cats.effect.{ContextShift, IO}
import cats.syntax.all._
import stryker4s.Stryker4s
import stryker4s.config._
import stryker4s.files.DiskFileIO
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher, SourceCollector}
import stryker4s.report._
import stryker4s.report.dashboard.DashboardConfigProvider
import stryker4s.run.process.ProcessRunner
import stryker4s.run.threshold.ScoreStatus
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend

abstract class Stryker4sRunner(implicit cs: ContextShift[IO]) {
  def run(): IO[ScoreStatus] = {
    implicit val config: Config = ConfigReader.readConfig()

    val collector = new FileCollector(ProcessRunner())

    resolveReporters().flatMap { reporters =>
      val stryker4s = new Stryker4s(
        collector,
        new Mutator(
          new MutantFinder(new MutantMatcher),
          new StatementTransformer,
          new MatchBuilder(mutationActivation)
        ),
        resolveRunner(collector, new AggregateReporter(reporters))
      )
      stryker4s.run()
    }
  }

  def resolveReporters()(implicit config: Config) =
    config.reporters.toList.traverse {
      case Console => IO(new ConsoleReporter())
      case Html    => IO(new HtmlReporter(new DiskFileIO()))
      case Json    => IO(new JsonReporter(new DiskFileIO()))
      case Dashboard =>
        AsyncHttpClientCatsBackend[IO]()
          .map { implicit backend =>
            new DashboardReporter(new DashboardConfigProvider(sys.env))
          }
    }

  def resolveRunner(collector: SourceCollector, reporter: Reporter)(implicit config: Config): MutantRunner

  def mutationActivation: ActiveMutationContext
}
