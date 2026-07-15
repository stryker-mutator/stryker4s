package stryker4s.sbt

import cats.data.NonEmptyList
import cats.effect.{Deferred, IO, Resource}
import cats.syntax.either.*
import fs2.io.file.Path
import sbt.Keys.*
import sbt.internal.LogManager
import sbt.{given, *}
import sjsonnew.*
import stryker4s.config.source.ConfigSource
import stryker4s.config.{Config, TestFilter}
import stryker4s.exception.TestSetupException
import stryker4s.extension.FileExtensions.*
import stryker4s.log.Logger
import stryker4s.model.{CompilerErrMsg, TestInterfaceMapper, TestRunnerId}
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.tree.InstrumenterOptions
import stryker4s.run.testrunner.ProcessTestRunner
import stryker4s.run.{Stryker4sRunner, TestRunner}
import stryker4s.sbt.runner.LegacySbtTestRunner
import xsbti.FileConverter

import java.io.{File as JFile, PrintStream}
import scala.concurrent.duration.FiniteDuration

import Stryker4sPlugin.autoImport.stryker

final case class StrykerSbtContext(
    state: State,
    javaHome: Option[File],
    targetProject: ProjectRef
)

/** This Runner run Stryker mutations in a single SBT session
  */
class Stryker4sSbtRunner(
    ctx: StrykerSbtContext,
    sharedTimeout: Deferred[IO, FiniteDuration],
    override val extraConfigSources: List[ConfigSource[IO]]
)(implicit
    log: Logger,
    conv: FileConverter
) extends Stryker4sRunner {

  def resolveTestRunners(
      tmpDir: Path
  )(implicit config: Config): Either[NonEmptyList[CompilerErrMsg], NonEmptyList[Resource[IO, TestRunner]]] = {
    def setupLegacySbtTestRunner(
        settings: Seq[Def.Setting[?]],
        extracted: Extracted
    ): NonEmptyList[Resource[IO, TestRunner]] = {
      log.warn(
        "Using the legacy sbt testrunner. Note: this mode is much slower, and does not support compiler error detection. Consider disabling 'legacy-test-runner' for a better experience."
      )

      val emptyLogManager =
        LogManager.defaultManager(ConsoleOut.printStreamOut(new PrintStream((_: Int) => {})))

      val fullSettings = settings ++ Seq(
        ctx.targetProject / logManager := {
          if ((stryker / logLevel).value == Level.Debug) logManager.value
          else emptyLogManager
        }
      )
      val newState = extracted.appendWithSession(fullSettings, ctx.state)

      NonEmptyList.of(Resource.pure(new LegacySbtTestRunner(newState, fullSettings, extracted, ctx.targetProject)))
    }

    def setupSbtTestRunner(
        settings: Seq[Def.Setting[?]],
        extracted: Extracted
    ): Either[NonEmptyList[CompilerErrMsg], NonEmptyList[Resource[IO, TestRunner]]] = {
      val stryker4sVersion = this.getClass().getPackage().getImplementationVersion()
      log.debug(s"Resolved stryker4s version $stryker4sVersion")

      val fullSettings = settings ++ Seq(
        ctx.targetProject / libraryDependencies +=
          "io.stryker-mutator" %% "stryker4s-testrunner" % stryker4sVersion,
        ctx.targetProject / resolvers ++=
          (if (stryker4sVersion.endsWith("-SNAPSHOT")) Seq(Resolver.sonatypeCentralSnapshots) else Seq.empty)
      )
      val newState = extracted.appendWithSession(fullSettings, ctx.state)

      def extractTaskValue[T](task: TaskKey[T]): T = {
        PluginCompat.runTask(task, newState) match {
          case Some(Right(result)) => result
          case other               =>
            log.debug(s"Expected task '${task.key.label}' to succeed, but got: $other")
            throw TestSetupException(task.key.label)
        }
      }

      // SBT returns any errors as a Incomplete case class, which can contain other Incomplete instances
      // You have to recursively search through them to get the real exception
      def getRootCause(i: Incomplete): Seq[Throwable] = {
        i.directCause match {
          case None =>
            i.causes.flatMap(getRootCause)
          case Some(cause) =>
            cause +: i.causes.flatMap(getRootCause)
        }
      }

      // See if the mutations compile, and if not extract the errors
      val compilerErrors = PluginCompat.runTask(ctx.targetProject / Compile / compile, newState) match {
        case Some(Left(cause)) =>
          val rootCauses = getRootCause(cause)
          rootCauses.foreach(t => log.debug(s"Compile failed with ${t.getClass().getName()} root cause: $t"))
          val compileErrors = rootCauses
            .collect { case e: xsbti.CompileFailed => e }
            .flatMap { exception =>
              exception.problems.flatMap { e =>
                for {
                  path <- e.position().sourceFile().asScala
                  pathStr = Path.fromNioPath(path.toPath()).relativePath(tmpDir).toString
                  line <- e.position().line().asScala
                  offset = e.position().offset().asScala.map(_.toInt)
                } yield CompilerErrMsg(e.message(), pathStr, line, offset)
              }
            }
            .toList

          NonEmptyList.fromList(compileErrors)
        case _ =>
          None
      }

      compilerErrors.toLeft {
        val classpath =
          PluginCompat.toNioPaths(extractTaskValue(ctx.targetProject / Test / fullClasspath)).map(Path.fromNioPath)

        val javaOpts = extractTaskValue(ctx.targetProject / Test / javaOptions)

        val frameworks = extractTaskValue(ctx.targetProject / Test / loadedTestFrameworks).values.toSeq
        if (frameworks.isEmpty)
          log.warn(
            "No test frameworks found via loadedTestFrameworks. " +
              "Will likely result in no tests being run and a NoCoverage result for all mutants."
          )

        val testGroups = extractTaskValue(ctx.targetProject / Test / testGrouping).map { group =>
          if (config.testFilter.isEmpty) group
          else {
            val testFilter = new TestFilter()
            val filteredTests = group.tests.filter(t => testFilter(t.name))
            new Tests.Group(name = group.name, tests = filteredTests, runPolicy = group.runPolicy)
          }
        }

        val concurrency = if (config.debug.debugTestRunner) {
          log.warn(
            "'debug.debug-test-runner' config is 'true', creating 1 test-runner with debug arguments enabled on port 8000."
          )
          1
        } else {
          log.info(s"Creating ${config.concurrency} test-runners")
          config.concurrency
        }

        val testRunnerIds = NonEmptyList.fromListUnsafe(
          (1 to concurrency).map(TestRunnerId(_)).toList
        )

        val apiTestGroups = TestInterfaceMapper.toApiTestGroups(frameworks, testGroups)

        testRunnerIds.map { id =>
          ProcessTestRunner.create(ctx.javaHome, classpath, javaOpts, apiTestGroups, id, sharedTimeout)
        }
      }
    }

    def targetProjectSessionOverrides(tmpDir: Path)(implicit config: Config): Seq[Def.Setting[?]] = {
      // Remove scalacOptions that are very likely to cause errors with generated code
      // https://github.com/stryker-mutator/stryker4s/issues/321
      val blocklistedScalacOptions = Seq(
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

      val filteredSystemProperties: Seq[String] = {
        // Matches strings that start with one of the options between brackets
        val regex = "^(java|sun|file|user|jna|os|sbt|jline|awt|graal|jdk|line\\.separator).*"
        for {
          (key, value) <- sys.props.toList.filterNot { case (key, _) => key.matches(regex) }
          param = s"-D$key=$value"
        } yield param
      }
      log.debug(s"System properties added to the forked JVM: ${filteredSystemProperties.mkString(", ")}")

      Seq(
        ctx.targetProject / scalacOptions --= blocklistedScalacOptions,
        ctx.targetProject / Test / fork := true,
        ctx.targetProject / Compile / unmanagedSourceDirectories ~= (_.map(tmpDirFor(_, tmpDir))),
        ctx.targetProject / Test / javaOptions ++= filteredSystemProperties
      ) ++ {
        if (config.testFilter.nonEmpty) {
          val testFilter = new TestFilter()
          Seq(ctx.targetProject / Test / testOptions := Seq(Tests.Filter(testFilter(_))))
        } else
          Nil
      }
    }

    def tmpDirFor(source: File, tmpDir: Path): JFile =
      Path.fromNioPath(source.toPath()).inSubDir(tmpDir).toNioPath.toFile().getAbsoluteFile()

    val sessionOverrides = targetProjectSessionOverrides(tmpDir)
    val extracted = Project.extract(ctx.state)

    if (config.legacyTestRunner) {
      // No compiler error handling in the legacy runner
      setupLegacySbtTestRunner(sessionOverrides, extracted).asRight
    } else
      setupSbtTestRunner(sessionOverrides, extracted)
  }

  override def instrumenterOptions(implicit config: Config): InstrumenterOptions =
    if (config.legacyTestRunner) {
      InstrumenterOptions.sysContext(ActiveMutationContext.sysProps)
    } else {
      InstrumenterOptions.testRunner
    }
}
