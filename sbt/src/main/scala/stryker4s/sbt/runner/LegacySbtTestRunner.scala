package stryker4s.sbt.runner

import cats.effect.IO
import stryker4s.log.Logger
import sbt.Keys._
import sbt.Tests.Output
import sbt._
import stryker4s.extension.exception.InitialTestRunFailedException
import stryker4s.model.{Error, Killed, Mutant, MutantRunResult, Survived}
import stryker4s.run.TestRunner

class LegacySbtTestRunner(initialState: State, settings: Seq[Def.Setting[_]], extracted: Extracted)(implicit
    log: Logger
) extends TestRunner {
  def initialTestRun(): IO[Boolean] = runTests(
    initialState,
    throw InitialTestRunFailedException(
      s"Unable to execute initial test run. Sbt is unable to find the task 'test'."
    ),
    onSuccess = true,
    onFailed = false
  )

  def runMutant(mutant: Mutant): IO[MutantRunResult] = {
    val mutationState =
      extracted.appendWithSession(settings :+ mutationSetting(mutant.id), initialState)
    runTests(
      mutationState,
      onError = {
        log.error(s"An unexpected error occurred while running mutation ${mutant.id}")
        Error(mutant)
      },
      onSuccess = Survived(mutant),
      onFailed = Killed(mutant)
    )
  }

  private def runTests[T](state: State, onError: => T, onSuccess: => T, onFailed: => T): IO[T] =
    IO(Project.runTask(executeTests in Test, state)) map {
      case Some((_, Value(Output(TestResult.Passed, _, _)))) => onSuccess
      case Some((_, Value(Output(TestResult.Failed, _, _)))) => onFailed
      case Some((_, Value(Output(TestResult.Error, _, _))))  => onFailed
      case _                                                 => onError
    }

  private def mutationSetting(mutation: Int): Def.Setting[_] =
    javaOptions in Test += s"-DACTIVE_MUTATION=${String.valueOf(mutation)}"
}
