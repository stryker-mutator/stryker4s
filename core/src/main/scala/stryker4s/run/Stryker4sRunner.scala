package stryker4s.run

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
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
import sttp.client3.SttpBackend
import sttp.client3.httpclient.fs2.HttpClientFs2Backend

import java.nio.file.Path

abstract class Stryker4sRunner(implicit log: Logger) {
  def run(): IO[ScoreStatus] = {
    implicit val config: Config = ConfigReader.readConfig()

    val collector = new FileCollector(ProcessRunner())

    val createTestRunnerPool = (path: Path) => ResourcePool(resolveTestRunners(path))

    val stryker4s = new Stryker4s(
      collector,
      new Mutator(
        new MutantFinder(new MutantMatcher),
        new StatementTransformer,
        resolveMatchBuilder
      ),
      new MutantRunner(createTestRunnerPool, collector, new AggregateReporter(resolveReporters()))
    )

    stryker4s.run()
  }

  def resolveReporters()(implicit config: Config): List[Reporter] =
    config.reporters.toList.map {
      case Console => new ConsoleReporter()
      case Html    => new HtmlReporter(new DiskFileIO())
      case Json    => new JsonReporter(new DiskFileIO())
      case Dashboard =>
        implicit val httpBackend: Resource[IO, SttpBackend[IO, Any]] =
          // Catch if the user runs the dashboard on Java <11
          try HttpClientFs2Backend.resource[IO]()
          catch {
            case e: BootstrapMethodError =>
              // Wrap in a UnsupportedOperationException because BootstrapMethodError will not be caught
              throw new UnsupportedOperationException(
                "Could not send results to dashboard. The dashboard reporter only supports JDK 11 or above. If you are running on a lower Java version please upgrade or disable the dashboard reporter.",
                e
              )
          }
        new DashboardReporter(new DashboardConfigProvider(sys.env))
    }

  def resolveMatchBuilder(implicit config: Config): MatchBuilder = new MatchBuilder(mutationActivation)

  def resolveTestRunners(tmpDir: Path)(implicit config: Config): NonEmptyList[Resource[IO, stryker4s.run.TestRunner]]

  def mutationActivation(implicit config: Config): ActiveMutationContext
}
