package stryker4s.sbt
import java.io.{File => JFile}
import java.nio.file.Path

import better.files.File
import sbt.Keys._
import sbt._
import stryker4s.config.Config
import stryker4s.extension.exception.{InitialTestRunFailedException, MutationRunFailedException, Stryker4sException}
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.MutantRunner
import stryker4s.run.process.ProcessRunner

class SbtMutantRunner(state: State, processRunner: ProcessRunner, sourceCollector: SourceCollector)(
    implicit config: Config)
    extends MutantRunner(processRunner, sourceCollector) {

  private lazy val filteredSystemProperties = {
    // Matches strings that start with one of the options between brackets
    val regex = "^(java|sun|file|user|jna|os|sbt|jline|awt|user|graal).*"

    val filteredProps =
      sys.props.toList
        .filterNot { case (key, _) => key.matches(regex) }
        .map { case (key, value) => s"-D$key=$value" }

    if (filteredProps.nonEmpty) {
      debug("System properties added to the forked JVM")
      filteredProps.foreach(debug(_))
    }
    filteredProps
  }

  private val settings: Seq[Def.Setting[_]] = Seq(
    fork in Test := true,
    javaOptions in Test ++= {
      filteredSystemProperties
    },
    scalaSource in Compile := tmpDirFor(Compile).value,
    scalaSource in Test := tmpDirFor(Test).value,
  )

  private val extracted = Project.extract(state)

  private val newState = extracted.appendWithoutSession(settings, state)

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

  private def tmpDirFor(conf: Configuration): Def.Initialize[JFile] = {
    val sourceDirDef = (scalaSource in conf)(_.absolutePath)
    val baseDirDef = (baseDirectory in conf)(_.absolutePath)

    sourceDirDef.zipWith(baseDirDef) { (sourceDir, baseDir) =>
      val relativePath = sourceDir diff baseDir

      tmpDir.toJava / relativePath
    }
  }
}
