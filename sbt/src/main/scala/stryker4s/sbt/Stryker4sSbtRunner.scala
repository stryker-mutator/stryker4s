package stryker4s.sbt

import java.io.{File => JFile, PrintStream}
import java.nio.file.Path

import cats.effect.{ContextShift, IO, Timer}
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
class Stryker4sSbtRunner(state: State)(implicit log: Logger, timer: Timer[IO], cs: ContextShift[IO])
    extends Stryker4sRunner {

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
          if ((logLevel in stryker).value == Level.Debug) logManager.value
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

      val classpath = extractTaskValue(fullClasspath in Test, "classpath").map(_.data.getPath())

      val javaOpts = extractTaskValue(javaOptions in Test, "javaOptions")

      val frameworks = extractTaskValue(loadedTestFrameworks in Test, "test frameworks").values.toSeq

      val testGroups = extractTaskValue(testGrouping in Test, "testGrouping")

      log.info(s"Creating ${config.concurrency} test-runners")
      val portStart = 13336
      val portRanges = (1 to config.concurrency).map(_ + portStart)

      Stream.emits(portRanges).flatMap { port =>
        Stream.resource(SbtTestRunner.create(classpath, javaOpts, frameworks, testGroups, port))
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
        fork in Test := true,
        scalaSource in Compile := tmpDirFor(Compile, tmpDir).value,
        javaOptions in Test ++= filteredSystemProperties
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
      (scalaSource in conf)(_.toPath())(source => (source inSubDir tmpDir).toFile())

    val (settings, extracted) = extractSbtProject(tmpDir)

    if (config.legacyTestRunner)
      setupLegacySbtTestRunner(settings, extracted)
    else
      setupSbtTestRunner(settings, extracted)
  }

}
