package stryker4s.run

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import fs2.io.file.Path
import stryker4s.config._
import stryker4s.files._
import stryker4s.log.Logger
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{MutantFinder, MutantMatcher}
import stryker4s.report._
import stryker4s.report.dashboard.DashboardConfigProvider
import stryker4s.run.process.ProcessRunner
import stryker4s.run.threshold.ScoreStatus
import stryker4s.Stryker4s
import stryker4s.model.CompilerErrMsg
import sttp.client3.SttpBackend
import sttp.client3.httpclient.fs2.HttpClientFs2Backend

abstract class Stryker4sRunner(implicit log: Logger) {
  def run(): IO[ScoreStatus] = {
    implicit val config: Config = ConfigReader.readConfig()

    val createTestRunnerPool = (path: Path) => resolveTestRunners(path).map(ResourcePool(_))

    val stryker4s = new Stryker4s(
      resolveMutatesFileSource,
      new Mutator(
        new MutantFinder(new MutantMatcher),
        new StatementTransformer,
        resolveMatchBuilder
      ),
      new MutantRunner(createTestRunnerPool, resolveFilesFileSource, new AggregateReporter(resolveReporters()))
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

  def resolveTestRunners(tmpDir: Path)(implicit
      config: Config
  ): Either[NonEmptyList[CompilerErrMsg], NonEmptyList[Resource[IO, stryker4s.run.TestRunner]]]

  def resolveMutatesFileSource(implicit config: Config): MutatesFileResolver =
    new GlobFileResolver(
      config.baseDir,
      if (config.mutate.nonEmpty) config.mutate else Seq("**/main/scala/**.scala")
    )

  def resolveFilesFileSource(implicit config: Config): FilesFileResolver = new ConfigFilesResolver(ProcessRunner())

  def mutationActivation(implicit config: Config): ActiveMutationContext
}
