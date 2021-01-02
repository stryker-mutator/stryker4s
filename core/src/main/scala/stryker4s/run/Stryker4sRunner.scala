package stryker4s.run

import java.nio.file.Path

import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.syntax.all._
import stryker4s.Stryker4s
import stryker4s.config._
import stryker4s.files.DiskFileIO
import stryker4s.log.Logger
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher}
import stryker4s.report._
import stryker4s.report.dashboard.DashboardConfigProvider
import stryker4s.run.process.ProcessRunner
import stryker4s.run.threshold.ScoreStatus
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend

abstract class Stryker4sRunner(implicit log: Logger, cs: ContextShift[IO], timer: Timer[IO]) {
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
        new MutantRunner(resolveTestRunner(_), collector, new AggregateReporter(reporters))
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

  def resolveMatchBuilder(implicit config: Config): MatchBuilder = new MatchBuilder(mutationActivation)

  def resolveTestRunner(tmpDir: Path)(implicit config: Config): Resource[IO, stryker4s.run.TestRunner]

  def mutationActivation(implicit config: Config): ActiveMutationContext
}
