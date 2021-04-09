package stryker4s.sbt

import java.io.{File => JFile, PrintStream}
import java.nio.file.Path

import cats.effect.{IO, Resource}
import fs2.Stream
import sbt.Keys._
import sbt._
import sbt.internal.LogManager
import stryker4s.config.{Config, TestFilter}
import stryker4s.extension.FileExtensions._
import stryker4s.extension.exception.TestSetupException
import stryker4s.log.Logger
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext
import stryker4s.mutants.applymutants.{ActiveMutationContext, CoverageMatchBuilder, MatchBuilder}
import stryker4s.run.{Stryker4sRunner, TestRunner}
import stryker4s.sbt.Stryker4sMain.autoImport.stryker
import stryker4s.sbt.runner.{LegacySbtTestRunner, SbtTestRunner}

/** This Runner run Stryker mutations in a single SBT session
  *
  * @param state SBT project state (contains all the settings about the project)
  */
class Stryker4sSbtRunner(state: State)(implicit log: Logger) extends Stryker4sRunner {

  override def resolveMatchBuilder(implicit config: Config): MatchBuilder =
    if (config.legacyTestRunner) new MatchBuilder(mutationActivation) else new CoverageMatchBuilder(mutationActivation)

  override def mutationActivation(implicit config: Config): ActiveMutationContext =
    if (config.legacyTestRunner) ActiveMutationContext.sysProps else ActiveMutationContext.testRunner

  def resolveTestRunners(tmpDir: Path)(implicit config: Config): Stream[IO, stryker4s.run.TestRunner] = {
    def setupLegacySbtTestRunner(
        settings: Seq[Def.Setting[_]],
        extracted: Extracted
    ): Stream[IO, TestRunner] = {
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

      Stream.emit(new LegacySbtTestRunner(newState, fullSettings, extracted))
    }

    def setupSbtTestRunner(
        settings: Seq[Def.Setting[_]],
        extracted: Extracted
    ): Stream[IO, TestRunner] = {
      val stryker4sVersion = this.getClass().getPackage().getImplementationVersion()
      log.debug(s"Resolved stryker4s version $stryker4sVersion")

      val fullSettings = settings ++ Seq(
        libraryDependencies +=
          "io.stryker-mutator" %% "sbt-stryker4s-testrunner" % stryker4sVersion
      )
      val newState = extracted.appendWithSession(fullSettings, state)
      def extractTaskValue[T](task: TaskKey[T], name: String) =
        Project.runTask(task, newState) match {
          case Some((_, Value(result))) => result
          case other =>
            log.debug(s"Expected $name but got $other")
            throw new TestSetupException(
              s"Could not setup mutation testing environment. Unable to resolve project $name. This could be due to compile errors or misconfiguration of Stryker4s. See debug logs for more information."
            )
        }

      val classpath = extractTaskValue(Test / fullClasspath, "classpath").map(_.data.getPath())

      val javaOpts = extractTaskValue(Test / javaOptions, "javaOptions")

      val frameworks = extractTaskValue(Test / loadedTestFrameworks, "test frameworks").values.toSeq

      val testGroups = extractTaskValue(Test / testGrouping, "testGrouping")

      log.info(s"Creating ${config.concurrency} test-runners")
      val portStart = 13336
      val portRanges = (1 to config.concurrency).map(_ + portStart)

      // Shared `FiniteDuration` to set the timeout on. Based on initial test-run duration
      Stream.eval(Deferred[IO, FiniteDuration]).flatMap { timeout =>
        Stream.emits(portRanges).flatMap { port =>
          Stream.resource(SbtTestRunner.create(classpath, javaOpts, frameworks, testGroups, port, timeout))
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

      val settings: Seq[Def.Setting[_]] = Seq(
        scalacOptions --= blocklistedScalacOptions,
        Test / fork := true,
        Compile / scalaSource := tmpDirFor(Compile, tmpDir).value,
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

    def tmpDirFor(conf: Configuration, tmpDir: Path): Def.Initialize[JFile] =
      (conf / scalaSource)(_.toPath())(source => (source inSubDir tmpDir).toFile())

    val (settings, extracted) = extractSbtProject(tmpDir)

    if (config.legacyTestRunner)
      setupLegacySbtTestRunner(settings, extracted)
    else
      setupSbtTestRunner(settings, extracted)
  }

}
