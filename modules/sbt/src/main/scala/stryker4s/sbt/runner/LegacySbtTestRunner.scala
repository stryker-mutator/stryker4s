package stryker4s.sbt.runner

import cats.effect.IO
import mutationtesting.{MutantResult, MutantStatus}
import sbt.Keys.*
import sbt.Tests.Output
import sbt.{given, *}
import stryker4s.sbt.PluginCompat
import stryker4s.exception.InitialTestRunFailedException
import stryker4s.log.Logger
import stryker4s.model.*
import stryker4s.run.TestRunner
import stryker4s.testrunner.api.TestFile

class LegacySbtTestRunner(initialState: State, settings: Seq[Def.Setting[?]], extracted: Extracted)(implicit
    log: Logger
) extends TestRunner {
  def initialTestRun(): IO[InitialTestRunResult] = runTests(
    initialState,
    throw InitialTestRunFailedException(
      s"Unable to execute initial test run. Sbt is unable to find the task 'test'."
    ),
    onSuccess = NoCoverageInitialTestRun(true),
    onFailed = NoCoverageInitialTestRun(false)
  )

  def runMutant(mutant: MutantWithId, testNames: Seq[TestFile]): IO[MutantResult] = {
    val mutationState =
      extracted.appendWithSession(settings :+ mutationSetting(mutant.id.value), initialState)
    runTests(
      mutationState,
      onError = {
        log.error(s"An unexpected error occurred while running mutation ${mutant.id}")
        mutant.toMutantResult(MutantStatus.RuntimeError)
      },
      onSuccess = mutant.toMutantResult(MutantStatus.Survived),
      onFailed = mutant.toMutantResult(MutantStatus.Killed)
    )
  }

  private def runTests[T](state: State, onError: => T, onSuccess: => T, onFailed: => T): IO[T] =
    IO(PluginCompat.runTask(Test / executeTests, state)) map {
      case Some(Right(Output(TestResult.Passed, _, _))) => onSuccess
      case Some(Right(Output(TestResult.Failed, _, _))) => onFailed
      case Some(Right(Output(TestResult.Error, _, _)))  => onFailed
      case _                                            => onError
    }

  private def mutationSetting(mutation: Int): Def.Setting[?] =
    Test / javaOptions += s"-DACTIVE_MUTATION=${String.valueOf(mutation)}"
}
