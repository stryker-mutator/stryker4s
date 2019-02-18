package stryker4s.sbt
import java.io.{File => JFile}
import java.nio.file.Path

import better.files.{File, _}
import sbt.Keys._
import sbt._
import stryker4s.config.Config
import stryker4s.extension.FileExtensions._
import stryker4s.extension.exception.{InitialTestRunFailedException, MutationRunFailedException, Stryker4sException}
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.MutantRunner
import stryker4s.run.process.ProcessRunner
import stryker4s.run.report.MutantRunReporter

class SbtMutantRunner(state: State, processRunner: ProcessRunner, sourceCollector: SourceCollector, reporter: MutantRunReporter)(
    implicit config: Config)
    extends MutantRunner(processRunner, sourceCollector, reporter) {

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

  private val settings: Seq[Def.Setting[_]] = Seq(
    fork in Test := true,
    scalaSource in Compile := tmpDirFor(Compile).value
  ) ++
    filteredSystemProperties.map(properties => {
      debug(s"System properties added to the forked JVM: ${properties.mkString(",")}")
      javaOptions in Test ++= properties
    })

  private val extracted = Project.extract(state)

  private val newState = extracted.appendWithSession(settings, state)

  override def runInitialTest(workingDir: File): Boolean = runTests(
    newState,
    InitialTestRunFailedException(s"Unable to execute initial test run. Sbt is unable to find the task 'test'."),
    onSuccess = true,
    onFailed = false
  )

  override def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult = {
    val mutationState = extracted.appendWithSession(settings :+ mutationSetting(mutant.id), newState)
    runTests(
      mutationState,
      MutationRunFailedException(s"An unexpected error occurred while running mutation ${mutant.id}"),
      Survived(mutant, subPath),
      Killed(mutant, subPath)
    )
  }

  /** Runs tests with the giving state, calls the corresponding parameter on each result
    */
  private def runTests[T](state: State, onError: => Stryker4sException, onSuccess: => T, onFailed: => T): T =
    Project.runTask(test in Test, state) match {
      case None                => throw onError
      case Some((_, Value(_))) => onSuccess
      case Some((_, Inc(_)))   => onFailed
    }

  private def mutationSetting(mutation: Int): Def.Setting[_] =
    javaOptions in Test += s"-DACTIVE_MUTATION=${String.valueOf(mutation)}"

  private def tmpDirFor(conf: Configuration): Def.Initialize[JFile] =
    (scalaSource in conf)(_.toScala)(source => (source inSubDir tmpDir).toJava)
}
