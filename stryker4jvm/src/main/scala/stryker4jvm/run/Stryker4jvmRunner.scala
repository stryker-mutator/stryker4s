package stryker4jvm.run

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import fs2.io.file.Path
import stryker4jvm.Stryker4jvm
import stryker4jvm.config.{Config, ConfigReader, Console, Dashboard, Html, Json}
import stryker4jvm.core.logging.Logger
import stryker4jvm.core.model.InstrumenterOptions
import stryker4jvm.extensions.Stryker4jvmCoreConversions.*
import stryker4jvm.files.{ConfigFilesResolver, DiskFileIO, FilesFileResolver, GlobFileResolver, MutatesFileResolver}
import stryker4jvm.logging.SttpLogWrapper
import stryker4jvm.model.CompilerErrMsg
import stryker4jvm.mutants.{Mutator, SupportedLanguageMutators}
import stryker4jvm.reporting.*
import stryker4jvm.reporting.dashboard.DashboardConfigProvider
import stryker4jvm.reporting.reporters.*
import stryker4jvm.run.process.ProcessRunner
import stryker4jvm.run.threshold.ScoreStatus
import sttp.client3.SttpBackend
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import sttp.client3.logging.LoggingBackend
import sttp.model.HeaderNames

abstract class Stryker4jvmRunner(implicit log: Logger) {
  def run(): IO[ScoreStatus] = {
    implicit val config: Config = ConfigReader.readConfig()
    SupportedLanguageMutators.languageRouter.values.foreach(mutator =>
      mutator.setLanguageConfig(config.asLanguageMutatorConfig)
    )

    val createTestRunnerPool = (path: Path) => resolveTestRunners(path).map(ResourcePool(_))
    val reporter = new AggregateReporter(resolveReporters())

    val stryker4jvm = new Stryker4jvm(
      resolveMutatesFileSource,
      new Mutator(
        SupportedLanguageMutators.languageRouter
      ),
      new MutantRunner(createTestRunnerPool, resolveFilesFileSource, new RollbackHandler(), reporter),
      reporter
    )

    stryker4jvm.run()
  }

  def resolveReporters()(implicit config: Config): List[IOReporter[Config]] =
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
        new DashboardReporter(new DashboardConfigProvider(sys.env))
    }

  def resolveTestRunners(tmpDir: Path)(implicit
      config: Config
  ): Either[NonEmptyList[CompilerErrMsg], NonEmptyList[Resource[IO, stryker4jvm.run.TestRunner]]]

  def resolveMutatesFileSource(implicit config: Config): MutatesFileResolver =
    new GlobFileResolver(
      config.baseDir,
      if (config.mutate.nonEmpty) config.mutate else SupportedLanguageMutators.mutatesFileSources
    )

  def resolveFilesFileSource(implicit config: Config): FilesFileResolver = new ConfigFilesResolver(ProcessRunner())

  def instrumenterOptions(implicit config: Config): InstrumenterOptions
}
