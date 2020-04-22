package stryker4s.sbt.runner

import java.io.{PrintStream, File => JFile}
import java.nio.file.Path

import better.files.{File, _}
import sbt.Keys._
import sbt._
import sbt.internal.LogManager
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.extension.exception.InitialTestRunFailedException
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.MutantRunner
import stryker4s.sbt.Stryker4sMain.autoImport.stryker
import sbt.Tests.Output

class SbtMutantRunner(state: State, sourceCollector: SourceCollector, reporter: Reporter)(implicit config: Config)
    extends MutantRunner(sourceCollector, reporter) {
  private lazy val filteredSystemProperties: Option[List[String]] = {
    // Matches strings that start with one of the options between brackets
    val regex = "^(java|sun|file|user|jna|os|sbt|jline|awt|graal).*"

    val filteredProps = for {
      (key, value) <- sys.props.toList.filterNot { case (key, _) => key.matches(regex) }
      param = s"-D$key=$value"
    } yield param

    filteredProps match {
      case Nil                => None
      case list: List[String] => Some(list)
    }
  }

  /** Remove scalacOptions that are very likely to cause errors with generated code
    * https://github.com/stryker-mutator/stryker4s/issues/321
    */
  private val blacklistedScalacOptions = Seq(
    "-Ywarn-unused:patvars",
    "-Ywarn-unused:locals",
    "-Xlint-unused"
  )

  private lazy val emptyLogManager =
    LogManager.defaultManager(ConsoleOut.printStreamOut(new PrintStream((_: Int) => {})))

  private val settings: Seq[Def.Setting[_]] = Seq(
    scalacOptions --= blacklistedScalacOptions,
    fork in Test := true,
    scalaSource in Compile := tmpDirFor(Compile).value,
    logManager := {
      if ((logLevel in stryker).value == Level.Debug) logManager.value
      else emptyLogManager
    }
  ) ++
    filteredSystemProperties.map(properties => {
      debug(s"System properties added to the forked JVM: ${properties.mkString(",")}")
      javaOptions in Test ++= properties
    })

  private val extracted = Project.extract(state)

  private val newState = extracted.appendWithSession(settings, state)

  override def runInitialTest(workingDir: File): Boolean = runTests(
    newState,
    throw InitialTestRunFailedException(s"Unable to execute initial test run. Sbt is unable to find the task 'test'."),
    onSuccess = true,
    onFailed = false
  )

  override def runMutant(mutant: Mutant, workingDir: File): Path => MutantRunResult = {
    val mutationState = extracted.appendWithSession(settings :+ mutationSetting(mutant.id), newState)
    runTests(
      mutationState, { p: Path =>
        error(s"An unexpected error occurred while running mutation ${mutant.id}")
        Error(mutant, p)
      },
      Survived(mutant, _),
      Killed(mutant, _)
    )
  }

  /** Runs tests with the giving state, calls the corresponding parameter on each result
    */
  private def runTests[T](state: State, onError: => T, onSuccess: => T, onFailed: => T): T =
    Project.runTask(executeTests in Test, state) match {
      case Some((_, Value(Output(TestResult.Passed, _, _)))) => onSuccess
      case Some((_, Value(Output(TestResult.Failed, _, _)))) => onFailed
      case _                                                 => onError
    }

  private def mutationSetting(mutation: Int): Def.Setting[_] =
    javaOptions in Test += s"-DACTIVE_MUTATION=${String.valueOf(mutation)}"

  private def tmpDirFor(conf: Configuration): Def.Initialize[JFile] =
    (scalaSource in conf)(_.toScala)(source => (source inSubDir tmpDir).toJava)
}
