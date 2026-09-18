package stryker4s.run

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import fs2.io.file.Path
import stryker4s.Stryker4s
import stryker4s.config.*
import stryker4s.config.source.ConfigSource
import stryker4s.files.*
import stryker4s.log.{Logger, SttpLogWrapper}
import stryker4s.model.CompilerErrMsg
import stryker4s.mutants.findmutants.{CustomMutatorLoader, MutantFinder, MutantMatcher, MutantMatcherImpl}
import stryker4s.mutants.tree.{InstrumenterOptions, MutantCollector, MutantInstrumenter}
import stryker4s.mutants.{Mutator, TreeTraverserImpl}
import stryker4s.report.*
import stryker4s.report.dashboard.DashboardConfigProvider
import stryker4s.run.threshold.{ScoreStatus, SuccessStatus}
import sttp.client4.Backend
import sttp.client4.httpclient.fs2.HttpClientFs2Backend
import sttp.client4.logging.{LogConfig, LoggingBackend}
import sttp.model.HeaderNames

abstract class Stryker4sRunner(implicit log: Logger) {
  def run(): IO[ScoreStatus] =
    ConfigLoader.loadAll[IO](extraConfigSources).flatMap { implicit config =>
      config.showHelpMessage match {
        case Some(helpMessage) => IO(log.info(helpMessage)).as(SuccessStatus)
        case None              => executeStryker
      }
    }

  private def executeStryker(implicit config: Config): IO[ScoreStatus] =
    resolveCustomMutators.use { customMutators =>
      val createTestRunnerPool = (path: Path) => resolveTestRunners(path).map(ResourcePool(_))
      val reporter = new AggregateReporter(resolveReporters())

      val instrumenter = new MutantInstrumenter(instrumenterOptions)

      val matcher: MutantMatcher.MutationMatcher =
        MutantMatcher.withCustomMutators(new MutantMatcherImpl().allMatchers, customMutators)

      val stryker4s = new Stryker4s(
        GlobFileResolver.forMutate(),
        new Mutator(
          new MutantFinder(),
          new MutantCollector(
            new TreeTraverserImpl(),
            new MutantMatcher {
              override def allMatchers: MutantMatcher.MutationMatcher = matcher
            }
          ),
          instrumenter
        ),
        new MutantRunner(
          createTestRunnerPool,
          GlobFileResolver.forFiles(),
          RollbackHandler(instrumenter),
          reporter
        ),
        reporter
      )

      stryker4s.run()
    }

  /** Loads `config.customMutators`, scoping the project classloader (if any is needed) as a `Resource` so it is closed
    * again once the mutation run completes. Short-circuits to an empty list without creating a classloader at all when
    * no custom mutators are configured, avoiding the classpath-resolution work that would otherwise happen on every
    * default (no-custom-mutators) run.
    *
    * The load itself is `IO.blocking` because reflectively resolving, linking and initializing the mutator classes
    * reads from the project's classpath (JARs and class files on disk).
    */
  private def resolveCustomMutators(implicit config: Config): Resource[IO, List[stryker4s.mutatorapi.CustomMutator]] =
    if (config.customMutators.isEmpty) Resource.pure(Nil)
    else
      customMutatorClassLoader.evalMap(classLoader =>
        IO.blocking(CustomMutatorLoader.load(config.customMutators, classLoader))
      )

  private def resolveReporters()(implicit config: Config): List[Reporter] =
    config.reporters.toList.map {
      case Console   => new ConsoleReporter()
      case Html      => new HtmlReporter(new DiskFileIO(), new DesktopFileIO())
      case Json      => new JsonReporter(new DiskFileIO())
      case Dashboard =>
        implicit val httpBackend: Resource[IO, Backend[IO]] =
          // Catch if the user runs the dashboard on Java <11
          try
            HttpClientFs2Backend
              .resource[IO]()
              .map(
                LoggingBackend(
                  _,
                  new SttpLogWrapper(),
                  LogConfig(
                    logResponseBody = true,
                    sensitiveHeaders = HeaderNames.SensitiveHeaders + "X-Api-Key"
                  )
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
        new DashboardReporter(DashboardConfigProvider[IO]())
    }

  def resolveTestRunners(tmpDir: Path)(implicit
      config: Config
  ): Either[NonEmptyList[CompilerErrMsg], NonEmptyList[Resource[IO, stryker4s.run.TestRunner]]]

  def instrumenterOptions(implicit config: Config): InstrumenterOptions

  def extraConfigSources: List[ConfigSource[IO]]

  /** The `ClassLoader` used to reflectively load [[stryker4s.mutatorapi.CustomMutator]]s configured via
    * `Config.customMutators`. Must be able to resolve classes on the target project's classpath.
    *
    * Scoped as a `Resource` so build-tool overrides that allocate a dedicated `URLClassLoader` per run (to expose the
    * project's own classpath) can release it — closing its underlying JAR/classpath file handles — once the mutation
    * run using it has completed, rather than leaking it for the lifetime of the (potentially long-lived) build-tool
    * process.
    */
  def customMutatorClassLoader: Resource[IO, ClassLoader] = Resource.pure(getClass.getClassLoader)
}
