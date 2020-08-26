package stryker4s.sbt.runner

import java.io.{File => JFile}
import java.nio.file.Path

import better.files.{File, _}
import cats.effect.{ContextShift, IO, Resource, Timer}
import sbt.Keys._
import sbt._
import stryker4s.config.{Config, TestFilter}
import stryker4s.extension.FileExtensions._
import stryker4s.extension.exception.TestSetupException
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.MutantRunner

class SbtMutantRunner(state: State, sourceCollector: SourceCollector, reporter: Reporter)(implicit
    config: Config,
    timer: Timer[IO],
    cs: ContextShift[IO]
) extends MutantRunner(sourceCollector, reporter) {
  type Context = SbtRunnerContext

  def initializeTestContext(tmpDir: File): Resource[IO, Context] = {
    val (classpath, javaOpts, frameworks, testGroups) = extractSbtContext(tmpDir)

    SbtTestRunner
      .create(classpath, javaOpts, frameworks, testGroups)
      .map(testRunner => SbtRunnerContext(testRunner, tmpDir))
  }

  private def extractSbtContext(tmpDir: File) = {

    // Remove scalacOptions that are very likely to cause errors with generated code
    // https://github.com/stryker-mutator/stryker4s/issues/321
    val blocklistedScalacOptions = Seq(
      "unused:patvars",
      "unused:locals",
      "unused:params",
      "unused:explicits"
      // -Ywarn for Scala 2.12, -W for Scala 2.13
    ).flatMap(opt => Seq(s"-Ywarn-$opt", s"-W$opt"))

    val stryker4sVersion = this.getClass().getPackage().getImplementationVersion()
    debug(s"Resolved stryker4s version $stryker4sVersion")

    val filteredSystemProperties: Seq[String] = {
      // Matches strings that start with one of the options between brackets
      val regex = "^(java|sun|file|user|jna|os|sbt|jline|awt|graal|jdk).*"
      for {
        (key, value) <- sys.props.toList.filterNot { case (key, _) => key.matches(regex) }
        param = s"-D$key=$value"
      } yield param
    }

    val settings: Seq[Def.Setting[_]] = Seq(
      scalacOptions --= blocklistedScalacOptions,
      fork in Test := true,
      scalaSource in Compile := tmpDirFor(Compile, tmpDir).value,
      libraryDependencies +=
        "io.stryker-mutator" %% "sbt-stryker4s-testrunner" % stryker4sVersion,
      javaOptions in Test ++= {
        debug(s"System properties added to the forked JVM: ${filteredSystemProperties.mkString(",")}")
        filteredSystemProperties
      }
    ) ++ {
      if (config.testFilter.nonEmpty) {
        val testFilter = new TestFilter
        Seq(Test / testOptions := Seq(Tests.Filter(testFilter.filter)))
      } else
        Nil
    }

    val extracted = Project.extract(state)

    val newState = extracted.appendWithSession(settings, state)
    def extractTaskValue[T](task: TaskKey[T], name: String) =
      Project.runTask(task, newState) match {
        case Some((_, Value(result))) => result
        case other =>
          throw new TestSetupException(
            s"Could not setup mutation testing environment. Expected $name, but got $other"
          )
      }

    val classpath = extractTaskValue(fullClasspath in Test, "classpath").map(_.data.getPath())

    val javaOpts = extractTaskValue(javaOptions in Test, "javaOptions")

    val frameworks = extractTaskValue(loadedTestFrameworks in Test, "test frameworks").values.toSeq

    val testGroups = extractTaskValue(testGrouping in Test, "testGrouping")

    (classpath, javaOpts, frameworks, testGroups)
  }

  override def runInitialTest(context: Context): IO[Boolean] =
    context.testRunner.initialTestRun()

  override def runMutant(mutant: Mutant, context: Context, subPath: Path): IO[MutantRunResult] =
    context.testRunner.runMutant(mutant, subPath)

  private def tmpDirFor(conf: Configuration, tmpDir: File): Def.Initialize[JFile] =
    (scalaSource in conf)(_.toScala)(source => (source inSubDir tmpDir).toJava)
}
