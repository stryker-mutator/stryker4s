package stryker4s.run

import cats.effect.{ContextShift, IO}
import cats.syntax.all._
import stryker4s.Stryker4s
import stryker4s.config._
import stryker4s.files.DiskFileIO
import stryker4s.log.Logger
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher, SourceCollector}
import stryker4s.report._
import stryker4s.report.dashboard.DashboardConfigProvider
import stryker4s.run.process.ProcessRunner
import stryker4s.run.threshold.ScoreStatus
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend

abstract class Stryker4sRunner(implicit log: Logger, cs: ContextShift[IO]) {
  def run(): IO[ScoreStatus] = {
    implicit val config: Config = ConfigReader.readConfig()

    val collector = new FileCollector(ProcessRunner())

    resolveReporters().flatMap { reporters =>
      val stryker4s = new Stryker4s(
        collector,
        new Mutator(
          new MutantFinder(new MutantMatcher),
          new StatementTransformer,
          resolveMatchBuilder
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

  def resolveMatchBuilder(implicit config: Config): MatchBuilder = new MatchBuilder(mutationActivation)

  def mutationActivation(implicit config: Config): ActiveMutationContext
}
