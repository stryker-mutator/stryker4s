package stryker4s.maven

import cats.data.NonEmptyList
import cats.effect.std.Dispatcher
import cats.effect.{Deferred, IO, Resource}
import cats.syntax.all.*
import fs2.io.file.Path
import org.apache.maven.project.MavenProject
import stryker4s.config.source.ConfigSource
import stryker4s.config.{Config, TestFilter}
import stryker4s.exception.TestSetupFailedException
import stryker4s.log.Logger
import stryker4s.maven.runner.{ArtifactResolver, MavenCompiler, MavenTestDiscovery, MavenTestSelection, ScalaVersions}
import stryker4s.model.{CompilerErrMsg, TestRunnerId}
import stryker4s.mutants.tree.InstrumenterOptions
import stryker4s.run.testrunner.ProcessTestRunner
import stryker4s.run.{Stryker4sRunner, TestRunner}

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*

/** Runs Stryker mutations in a single forked test-runner process per concurrency slot, communicating over a socket.
  *
  * The classpath, test frameworks and compiled (mutated) classes are extracted from Maven directly; the test-runner
  * back end is the same one sbt and Mill use ([[stryker4s.run.testrunner.ProcessTestRunner]]).
  */
class Stryker4sMavenRunner(
    project: MavenProject,
    resolver: ArtifactResolver,
    compiler: MavenCompiler,
    sharedTimeout: Deferred[IO, FiniteDuration],
    dispatcher: Dispatcher[IO]
)(using log: Logger)
    extends Stryker4sRunner {

  override def instrumenterOptions(using Config): InstrumenterOptions =
    InstrumenterOptions.testRunner

  override def extraConfigSources: List[ConfigSource[IO]] = List(new MavenConfigSource[IO](project))

  override def resolveTestRunners(
      tmpDir: Path
  )(using config: Config): Either[NonEmptyList[CompilerErrMsg], NonEmptyList[Resource[IO, TestRunner]]] = {
    val testRunnersF = for {
      testRunnerClasspath <- resolveTestRunnerArtifact()
      compileResult <- compiler.compile(tmpDir, testRunnerClasspath)

      result <- compileResult.traverse(output =>
        val depClasspath = project
          .getTestClasspathElements()
          .asScala
          .toSeq
          .map(Path(_))
          .filterNot(MavenCompiler.isOriginalOutput(project))

        val classpath =
          Seq(output.testClasses, output.mainClasses) ++ MavenCompiler.resourceDirs(project) ++ depClasspath
        MavenTestDiscovery.discover(Seq(output.testClasses), classpath).map { discovered =>
          // Restrict to the frameworks enabled in pom.xml
          val selected = MavenTestSelection
            .fromProject(project)
            .fold(discovered)(includeFramework => discovered.filter(group => includeFramework(group.frameworkClass)))

          if selected.isEmpty then
            log.warn(
              "No test frameworks were selected based on the project's test plugin configuration " +
                "(surefire/scalatest-maven-plugin). All mutants will be reported as NoCoverage. " +
                "Check that your test plugin is enabled."
            )
          val testGroups = if config.testFilter.isEmpty then selected
          else {
            val testFilter = new TestFilter()
            selected.map(group =>
              group.copy(taskDefs = group.taskDefs.filter(taskDef => testFilter(taskDef.fullyQualifiedName)))
            )
          }
          val concurrency = if config.debug.debugTestRunner then {
            log.warn(
              "'debug.debug-test-runner' config is 'true', creating 1 test-runner with debug arguments enabled on port 8000."
            )
            1
          } else {
            log.info(s"Creating ${config.concurrency} test-runners")
            config.concurrency
          }
          val testRunnerIds = NonEmptyList.fromListUnsafe((1 to concurrency).map(TestRunnerId(_)).toList)
          testRunnerIds.map { id =>
            ProcessTestRunner.create(
              javaHome = None,
              classpath = classpath ++ testRunnerClasspath,
              javaOpts = forkArgs,
              testGroups = testGroups,
              id = id,
              timeout = sharedTimeout
            )
          }
        }
      )
    } yield result
    dispatcher.unsafeRunSync(testRunnersF)
  }

  /** Resolve the `stryker4s-testrunner` artifact (added to the test-runner process classpath) for the target project's
    * Scala version.
    */
  private def resolveTestRunnerArtifact(): IO[Seq[Path]] = {
    val version = Option(getClass().getPackage().getImplementationVersion())
      .getOrElse(throw TestSetupFailedException("Could not resolve the stryker4s version from the plugin package"))
    log.debug(s"Resolved stryker4s version $version")
    val binaryVersion = ScalaVersions.binaryVersion(ScalaVersions.fullVersionUnsafe(project))
    resolver
      .resolveTransitively(s"io.stryker-mutator:stryker4s-testrunner_$binaryVersion:$version")
  }

  /** JVM options for the forked test-runner: the parent's system properties, minus platform/internal ones (mirrors the
    * sbt runner). Surefire `argLine` support can be added later.
    */
  private def forkArgs: Seq[String] = {
    val regex = "^(java|sun|file|user|jna|os|sbt|jline|awt|graal|jdk|line\\.separator).*"
    val props = sys.props.toList.collect {
      case (key, value) if !key.matches(regex) => s"-D$key=$value"
    }
    log.debug(s"System properties added to the forked JVM: ${props.mkString(", ")}")
    props
  }
}
