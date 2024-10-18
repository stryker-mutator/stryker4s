package stryker4s.sbt

import cats.data.NonEmptyList
import cats.effect.{Deferred, IO, Resource}
import cats.syntax.either.*
import com.comcast.ip4s.Port
import fs2.io.file.Path
import sbt.Keys.*
import sbt.internal.LogManager
import sbt.{given, *}
import stryker4s.PluginCompat
import stryker4s.config.source.ConfigSource
import stryker4s.config.{Config, TestFilter}
import stryker4s.exception.TestSetupException
import stryker4s.extension.FileExtensions.*
import stryker4s.log.Logger
import stryker4s.model.CompilerErrMsg
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.tree.InstrumenterOptions
import stryker4s.run.{Stryker4sRunner, TestRunner}
import stryker4s.sbt.runner.{LegacySbtTestRunner, SbtTestRunner}
import xsbti.FileConverter

import java.io.{File as JFile, PrintStream}
import scala.concurrent.duration.FiniteDuration

import Stryker4sPlugin.autoImport.stryker

/** This Runner run Stryker mutations in a single SBT session
  *
  * @param state
  *   SBT project state (contains all the settings about the project)
  */
class Stryker4sSbtRunner(
    state: State,
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
      log.info("Using the legacy sbt testrunner")

      val emptyLogManager =
        LogManager.defaultManager(ConsoleOut.printStreamOut(new PrintStream((_: Int) => {})))

      val fullSettings = settings ++ Seq(
        logManager := {
          if ((stryker / logLevel).value == Level.Debug) logManager.value
          else emptyLogManager
        }
      )
      val newState = extracted.appendWithSession(fullSettings, state)

      NonEmptyList.of(Resource.pure(new LegacySbtTestRunner(newState, fullSettings, extracted)))
    }

    def setupSbtTestRunner(
        settings: Seq[Def.Setting[?]],
        extracted: Extracted
    ): Either[NonEmptyList[CompilerErrMsg], NonEmptyList[Resource[IO, TestRunner]]] = {
      val stryker4sVersion = this.getClass().getPackage().getImplementationVersion()
      log.debug(s"Resolved stryker4s version $stryker4sVersion")

      val fullSettings = settings ++ Seq(
        libraryDependencies += "io.stryker-mutator" %% "stryker4s-sbt-testrunner" % stryker4sVersion,
        resolvers ++= (if (stryker4sVersion.endsWith("-SNAPSHOT")) Resolver.sonatypeOssRepos("snapshot")
                       else Seq.empty)
      )
      val newState = extracted.appendWithSession(fullSettings, state)

      def extractTaskValue[T](task: TaskKey[T]): T = {
        PluginCompat.runTask(task, newState) match {
          case Some(Right(result)) => result
          case other =>
            log.debug(s"Expected ${task.key.label} but got $other")
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
      val compilerErrors = PluginCompat.runTask(Compile / compile, newState) match {
        case Some(Left(cause)) =>
          val rootCauses = getRootCause(cause)
          rootCauses.foreach(t => log.debug(s"Compile failed with ${t.getClass().getName()} root cause: $t"))
          val compileErrors = rootCauses
            .collect { case e: xsbti.CompileFailed => e }
            .flatMap { exception =>
              exception.problems.flatMap { e =>
                for {
                  path <- e.position().sourceFile().asScala
                  pathStr = tmpDir.relativize(Path(path.absolutePath)).toString
                  line <- e.position().line().asScala
                } yield CompilerErrMsg(e.message(), pathStr, line)
              }
            }
            .toList

          NonEmptyList.fromList(compileErrors)
        case _ =>
          None
      }

      compilerErrors.toLeft {
        val classpath = stryker4s.PluginCompat.toNioPaths(extractTaskValue(Test / fullClasspath))

        val javaOpts = extractTaskValue(Test / javaOptions)

        val frameworks = extractTaskValue(Test / loadedTestFrameworks).values.toSeq
        if (frameworks.isEmpty)
          log.warn(
            "No test frameworks found via loadedTestFrameworks. " +
              "Will likely result in no tests being run and a NoCoverage result for all mutants."
          )

        val testGroups = extractTaskValue(Test / testGrouping).map { group =>
          if (config.testFilter.isEmpty) group
          else {
            val testFilter = new TestFilter()
            val filteredTests = group.tests.filter(t => testFilter.filter(t.name))
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

        val portStart = 13336
        val portRanges = NonEmptyList.fromListUnsafe(
          (1 to concurrency).map(p => Port.fromInt(p + portStart).get).toList
        )

        portRanges.map { port =>
          SbtTestRunner.create(classpath, javaOpts, frameworks, testGroups, port, sharedTimeout)
        }
      }
    }

    def extractSbtProject(tmpDir: Path)(implicit config: Config) = {
      // Remove scalacOptions that are very likely to cause errors with generated code
      // https://github.com/stryker-mutator/stryker4s/issues/321
      val blocklistedScalacOptions = Seq(
        "unused:patvars",
        "unused:locals",
        "unused:params",
        "unused:explicits"
        // -Ywarn for Scala 2.12, -W for Scala 2.13
      ).flatMap(opt => Seq(s"-Ywarn-$opt", s"-W$opt"))

      val filteredSystemProperties: Seq[String] = {
        // Matches strings that start with one of the options between brackets
        val regex = "^(java|sun|file|user|jna|os|sbt|jline|awt|graal|jdk).*"
        for {
          (key, value) <- sys.props.toList.filterNot { case (key, _) => key.matches(regex) }
          param = s"-D$key=$value"
        } yield param
      }
      log.debug(s"System properties added to the forked JVM: ${filteredSystemProperties.mkString(",")}")

      val settings: Seq[Def.Setting[?]] = Seq(
        scalacOptions --= blocklistedScalacOptions,
        Test / fork := true,
        Compile / unmanagedSourceDirectories ~= (_.map(tmpDirFor(_, tmpDir))),
        Test / javaOptions ++= filteredSystemProperties
      ) ++ {
        if (config.testFilter.nonEmpty) {
          val testFilter = new TestFilter()
          Seq(Test / testOptions := Seq(Tests.Filter(testFilter.filter)))
        } else
          Nil
      }

      (settings, Project.extract(state))
    }

    def tmpDirFor(source: File, tmpDir: Path): JFile =
      (Path.fromNioPath(source.toPath()) inSubDir tmpDir).toNioPath.toFile()

    val (settings, extracted) = extractSbtProject(tmpDir)

    if (config.legacyTestRunner) {
      // No compiler error handling in the legacy runner
      setupLegacySbtTestRunner(settings, extracted).asRight
    } else
      setupSbtTestRunner(settings, extracted)
  }

  override def instrumenterOptions(implicit config: Config): InstrumenterOptions =
    if (config.legacyTestRunner) {
      InstrumenterOptions.sysContext(ActiveMutationContext.sysProps)
    } else {
      InstrumenterOptions.testRunner
    }

}
