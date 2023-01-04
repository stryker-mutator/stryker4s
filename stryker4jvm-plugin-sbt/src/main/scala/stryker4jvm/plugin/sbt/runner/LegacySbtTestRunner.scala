package stryker4jvm.plugin.sbt.runner

import cats.effect.IO
import mutationtesting.{MutantResult, MutantStatus}
import sbt.*
import sbt.Keys.*
import sbt.Tests.Output
import stryker4jvm.core.logging.Logger
import stryker4jvm.core.model.{AST, MutantWithId}
import stryker4jvm.exception.InitialTestRunFailedException
import stryker4jvm.extensions.MutantExtensions.ToMutantResultExtension
import stryker4jvm.model.*
import stryker4jvm.run.TestRunner

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

  def runMutant(mutant: MutantWithId[AST], testNames: Seq[String]): IO[MutantResult] = {
    val mutationState =
      extracted.appendWithSession(settings :+ mutationSetting(mutant.id), initialState)
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
    IO(Project.runTask(Test / executeTests, state)) map {
      case Some((_, Value(Output(TestResult.Passed, _, _)))) => onSuccess
      case Some((_, Value(Output(TestResult.Failed, _, _)))) => onFailed
      case Some((_, Value(Output(TestResult.Error, _, _))))  => onFailed
      case _                                                 => onError
    }

  private def mutationSetting(mutation: Int): Def.Setting[?] =
    Test / javaOptions += s"-DACTIVE_MUTATION=${String.valueOf(mutation)}"
}
