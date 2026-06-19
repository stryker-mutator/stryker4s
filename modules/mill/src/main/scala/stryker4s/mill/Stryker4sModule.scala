package stryker4s.mill

import cats.effect.unsafe.IORuntime
import cats.effect.{Deferred, IO}
import cats.syntax.all.*
import mill.*
import mill.api.TaskCtx
import mill.javalib.api.JvmWorkerUtil
import mill.javalib.{Dep, TestModule}
import mill.scalalib.*
import stryker4s.config.codec.CirisConfigDecoders
import stryker4s.config.source.CliConfigSource
import stryker4s.log.{Logger, MillLogger}
import stryker4s.mill.runner.{MillTestDiscovery, Stryker4sMillRunner, StrykerMillContext}
import stryker4s.run.threshold.ErrorStatus
import upickle.ReadWriter

import scala.concurrent.duration.FiniteDuration
import scala.meta.{dialects, Dialect}

/** Mix this trait into a `ScalaModule` to add the `stryker` command, which runs Stryker4s mutation testing on the
  * module.
  *
  * Tests are run with the test module of this module (e.g. `object test extends ScalaTests`), which is looked up
  * automatically. Override `strykerTestModule` if it cannot be found.
  */
trait Stryker4sModule extends ScalaModule {

  /** The test module used to run tests during mutation testing. Defaults to the first child test module (e.g. `object
    * test extends ScalaTests`).
    */
  def strykerTestModule: TestModule = {
    val candidates = this.getClass.getMethods
      .filter(m =>
        m.getParameterCount == 0 && m.getName != "strykerTestModule" &&
          classOf[TestModule].isAssignableFrom(m.getReturnType)
      )
      .sortBy(_.getName)
      .toList

    candidates match {
      case single :: Nil => single.invoke(this).asInstanceOf[TestModule]
      case Nil           =>
        throw RuntimeException(
          s"No test module found for module '$this'. Override with `def strykerTestModule = myTestModule` to point Stryker4s to the test module to run tests with."
        )
      case multiple =>
        throw RuntimeException(
          s"Multiple test modules found for module '$this' (${multiple.map(_.getName).mkString(", ")}). " +
            "Override with `def strykerTestModule = myTestModule` to point Stryker4s to the test module to run tests with."
        )
    }
  }

  /** Pattern(s) of files to mutate. Defaults to all Scala files in `sources` */
  def strykerMutate: T[Option[Seq[String]]] = sourceGlobs(sources().map(_.path), "**.scala").some

  /** Pattern(s) of files to include in mutation testing. Defaults to all files in `sources` */
  def strykerFiles: T[Option[Seq[String]]] = sourceGlobs(sources().map(_.path), "**").some

  /** Filter out tests to run */
  def strykerTestFilter: T[Option[Seq[String]]] = None

  /** Reporter(s) to use */
  def strykerReporters: T[Option[Seq[String]]] = None

  /** Mutations to exclude from mutation testing */
  def strykerExcludedMutations: T[Option[Seq[String]]] = None

  /** Threshold for high mutation score */
  def strykerThresholdsHigh: T[Option[Int]] = None

  /** Threshold for low mutation score */
  def strykerThresholdsLow: T[Option[Int]] = None

  /** Threshold score for breaking the build */
  def strykerThresholdsBreak: T[Option[Int]] = None

  /** Base-url for the dashboard reporter */
  def strykerDashboardBaseUrl: T[Option[String]] = None

  /** Type of dashboard report (`full`, `mutationScoreOnly`) */
  def strykerDashboardReportType: T[Option[String]] = None

  /** Dashboard project identifier. Format of `gitProvider/organization/repository` */
  def strykerDashboardProject: T[Option[String]] = None

  /** Dashboard version identifier */
  def strykerDashboardVersion: T[Option[String]] = None

  /** Dashboard module identifier. Defaults to `artifactName` */
  def strykerDashboardModule: T[Option[String]] = artifactName().some

  /** Timeout for a single mutation test run. timeoutForTestRun = netTime * timeoutFactor + timeout */
  def strykerTimeout: T[Option[FiniteDuration]] = None

  /** Timeout factor for a single mutation test run. timeoutForTestRun = netTime * timeoutFactor + timeout */
  def strykerTimeoutFactor: T[Option[Double]] = None

  /** Restart the test-runner after every `n` runs */
  def strykerMaxTestRunnerReuse: T[Option[Int]] = None

  /** Dialect for parsing Scala files (e.g. `scala213source3`). Defaults to a dialect derived from `scalaVersion` */
  def strykerScalaDialect: T[Option[Dialect]] = strykerDialect(scalaVersion(), scalacOptions()).some

  /** Number of test-runners to start */
  def strykerConcurrency: T[Option[Int]] = None

  /** Log test-runner output to debug log */
  def strykerDebugLogTestRunnerStdout: T[Option[Boolean]] = None

  /** Pass JVM debugging parameters to the test-runner */
  def strykerDebugDebugTestRunner: T[Option[Boolean]] = None

  /** Force temporary dir to a static path (`target/stryker4s-tmpDir`) */
  def strykerStaticTmpDir: T[Option[Boolean]] = None

  /** Remove the tmpDir after a successful mutation test run */
  def strykerCleanTmpDir: T[Option[Boolean]] = None

  /** Open the report after a mutation test run */
  def strykerOpenReport: T[Option[Boolean]] = None

  /** Run Stryker4s mutation testing. */
  def stryker(args: String*): Command[Unit] = Task.Command {

    given Logger = new MillLogger(Task.log, Task.env)
    given IORuntime = IORuntime.global

    val testRunnerClasspath = resolveTestRunnerArtifact()()
    val testRunClasspath = strykerTestModule.runClasspath().map(_.path)
    val testGroups = MillTestDiscovery.discover(
      strykerTestModule.testFramework(),
      strykerTestModule.testClasspath().map(_.path),
      testRunClasspath
    )

    val context = StrykerMillContext(
      taskCtx = summon[TaskCtx],
      worker = jvmWorker().worker(),
      upstreamCompileOutput = upstreamCompileOutput(),
      sourceFiles = allSourceFiles().map(_.path),
      compileClasspath = compileClasspath().map(_.path),
      javaHome = javaHome().map(_.path),
      javacOptions = javacOptions() ++ mandatoryJavacOptions(),
      scalaVersion = scalaVersion(),
      scalacOptions = allScalacOptions().filterNot(blocklistedScalacOptions.contains),
      compilerClasspath = scalaCompilerClasspath(),
      scalacPluginClasspath = scalacPluginClasspath(),
      compilerBridge = scalaCompilerBridge(),
      testRunClasspath = testRunClasspath,
      forkArgs = strykerTestModule.forkArgs(),
      testGroups = testGroups,
      testRunnerClasspath = testRunnerClasspath
    )

    val millConfigSource = new MillConfigSource[IO](
      baseDirValue = fs2.io.file.Path.fromNioPath(moduleDir.toNIO),
      mutateValue = strykerMutate(),
      filesValue = strykerFiles(),
      testFilterValue = strykerTestFilter(),
      reportersValue = strykerReporters(),
      excludedMutationsValue = strykerExcludedMutations(),
      thresholdsHighValue = strykerThresholdsHigh(),
      thresholdsLowValue = strykerThresholdsLow(),
      thresholdsBreakValue = strykerThresholdsBreak(),
      dashboardBaseUrlValue = strykerDashboardBaseUrl(),
      dashboardReportTypeValue = strykerDashboardReportType(),
      dashboardProjectValue = strykerDashboardProject(),
      dashboardVersionValue = strykerDashboardVersion(),
      dashboardModuleValue = strykerDashboardModule(),
      timeoutValue = strykerTimeout(),
      timeoutFactorValue = strykerTimeoutFactor(),
      maxTestRunnerReuseValue = strykerMaxTestRunnerReuse(),
      scalaDialectValue = strykerScalaDialect(),
      concurrencyValue = strykerConcurrency(),
      debugLogTestRunnerStdoutValue = strykerDebugLogTestRunnerStdout(),
      debugDebugTestRunnerValue = strykerDebugDebugTestRunner(),
      staticTmpDirValue = strykerStaticTmpDir(),
      cleanTmpDirValue = strykerCleanTmpDir(),
      openReportValue = strykerOpenReport()
    )
    val cliConfigSource = new CliConfigSource[IO](args)

    val scoreStatus = Deferred[IO, FiniteDuration] // Create shared timeout between test-runners
      .map(new Stryker4sMillRunner(context, _, List(millConfigSource, cliConfigSource)))
      .flatMap(_.run())
      .unsafeRunSync()

    scoreStatus match {
      case ErrorStatus => Task.fail("Mutation score is below configured threshold")
      case _           => ()
    }
  }

  /** Resolve the `stryker4s-testrunner` artifact (which is build-tool agnostic, despite its name) that is added to the
    * classpath of the test-runner process.
    */
  private def resolveTestRunnerArtifact(): Task[Seq[os.Path]] = Task.Anon {
    val stryker4sVersion = Version.pkgVersion.getOrElse(Task.fail("Could not resolve stryker4s version"))
    Task.log.debug(s"Resolved stryker4s version $stryker4sVersion")
    val scalaBinaryVersion = JvmWorkerUtil.scalaBinaryVersion(scalaVersion())
    val testRunnerDep: Dep =
      Dep.parse(s"io.stryker-mutator:stryker4s-testrunner_$scalaBinaryVersion:$stryker4sVersion")

    defaultResolver().classpath(Seq(testRunnerDep)).map(_.path)
  }

  /** Glob patterns (relative to `moduleDir`) for all source directories of this module */
  private def sourceGlobs(sourceDirs: Seq[os.Path], postfix: String): Seq[String] =
    sourceDirs.collect {
      case dir if dir.startsWith(moduleDir) => (dir.relativeTo(moduleDir).segments :+ postfix).mkString("/")
    }

  /** The scalameta dialect for the configured Scala version */
  private def strykerDialect(scalaVersion: String, scalacOptions: Seq[String]): Dialect = {
    val hasSource3 = scalacOptions.exists(_.startsWith("-Xsource:3"))
    def reader(major: String, minor: String, source3: Boolean) =
      dialectDecoders.dialectReader
        .decode(none, s"scala$major$minor${if source3 then "source3" else ""}")
        .toOption

    scalaVersion.split('.').toList match {
      case "3" :: minor :: _   => reader("3", minor, false).getOrElse(dialects.Scala3)
      case major :: minor :: _ =>
        reader(major, minor, hasSource3)
          .getOrElse(if hasSource3 then dialects.Scala213Source3 else dialects.Scala213)
      case _ => if hasSource3 then dialects.Scala213Source3 else dialects.Scala213
    }
  }

  private object dialectDecoders extends CirisConfigDecoders

  // Remove scalacOptions that are very likely to cause errors with generated code
  // https://github.com/stryker-mutator/stryker4s/issues/321
  private def blocklistedScalacOptions: Seq[String] =
    Seq(
      "unused:patvars",
      "unused:locals",
      "unused:params",
      "unused:explicits"
      // -Ywarn for Scala 2.12, -W for Scala 2.13
    ).flatMap(opt => Seq(s"-Ywarn-$opt", s"-W$opt")) ++ Seq(
      // Disable fatal warnings, as they will cause a lot of mutation switching statements to not compile
      "-Xfatal-warnings",
      "-Werror",
      "-Ycheck-all-patmat"
    )

  given ReadWriter[Option[Dialect]] = upickle
    .readwriter[String]
    .bimap(_.map(_.toString.toLowerCase()).orEmpty, s => dialectDecoders.dialectReader.decode(None, s).toOption)
}

private[mill] object Version {

  /** Separate out of the [[Stryker4sModule]] to get the package of stryker4s, not the build object
    */
  def pkgVersion = Option(this.getClass.getPackage().getImplementationVersion())

}
