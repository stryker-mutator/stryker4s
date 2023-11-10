package stryker4s.run

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import fs2.io.file.Path
import stryker4s.Stryker4s
import stryker4s.config.*
import stryker4s.files.*
import stryker4s.log.{Logger, SttpLogWrapper}
import stryker4s.model.CompilerErrMsg
import stryker4s.mutants.findmutants.{MutantFinder, MutantMatcherImpl}
import stryker4s.mutants.tree.{InstrumenterOptions, MutantCollector, MutantInstrumenter}
import stryker4s.mutants.{Mutator, TreeTraverserImpl}
import stryker4s.report.*
import stryker4s.report.dashboard.DashboardConfigProvider
import stryker4s.run.process.ProcessRunner
import stryker4s.run.threshold.ScoreStatus
import sttp.client3.SttpBackend
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import sttp.client3.logging.LoggingBackend
import sttp.model.HeaderNames

abstract class Stryker4sRunner(implicit log: Logger) {
  def run(): IO[ScoreStatus] = {
    implicit val config: Config = ConfigReader.readConfig()

    val createTestRunnerPool = (path: Path) => resolveTestRunners(path).map(ResourcePool(_))
    val reporter = new AggregateReporter(resolveReporters())

    val instrumenter = new MutantInstrumenter(instrumenterOptions)
    val stryker4s = new Stryker4s(
      resolveMutatesFileSource,
      new Mutator(
        new MutantFinder(),
        new MutantCollector(new TreeTraverserImpl(), new MutantMatcherImpl()),
        instrumenter
      ),
      new MutantRunner(createTestRunnerPool, resolveFilesFileSource, new RollbackHandler(instrumenter), reporter),
      reporter
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
          try
            HttpClientFs2Backend
              .resource[IO]()
              .map(
                LoggingBackend(
                  _,
                  new SttpLogWrapper(),
                  logResponseBody = true,
                  sensitiveHeaders = HeaderNames.SensitiveHeaders + "X-Api-Key"
                )
              )
          catch {
            case e: BootstrapMethodError =>
              // Wrap in a UnsupportedOperationException because BootstrapMethodError will not be caught
              Resource.raiseError[IO, Nothing, Throwable](
                new UnsupportedOperationException(
                  "Could not send results to dashboard. The dashboard reporter only supports JDK 11 or above. If you are running on a lower Java version please upgrade or disable the dashboard reporter.",
                  e
                )
              )
          }
        new DashboardReporter(new DashboardConfigProvider[IO]())
    }

  def resolveTestRunners(tmpDir: Path)(implicit
      config: Config
  ): Either[NonEmptyList[CompilerErrMsg], NonEmptyList[Resource[IO, stryker4s.run.TestRunner]]]

  def resolveMutatesFileSource(implicit config: Config): MutatesFileResolver =
    new GlobFileResolver(
      config.baseDir,
      if (config.mutate.nonEmpty) config.mutate else Seq("**/main/scala/**.scala")
    )

  def resolveFilesFileSource(implicit config: Config): FilesFileResolver = new ConfigFilesResolver(ProcessRunner())

  def instrumenterOptions(implicit config: Config): InstrumenterOptions
}
