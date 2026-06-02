package stryker4s.testutil.stubs

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.either.*
import fs2.io.file.Path
import mutationtesting.{MutantResult, MutantStatus}
import stryker4s.model.*
import stryker4s.run.{ResourcePool, TestRunner, TestRunnerPool}
import stryker4s.testrunner.api.TestFile
import stryker4s.testutil.TestData

class TestRunnerStub(
    results: Seq[() => MutantResult],
    initialTestRunResult: InitialTestRunResult = NoCoverageInitialTestRun(true)
) extends TestRunner {
  private val stream = Iterator.from(0)

  override def initialTestRun(): IO[InitialTestRunResult] =
    IO.pure(initialTestRunResult)

  override def runMutant(mutant: MutantWithId, testNames: Seq[TestFile]): IO[MutantResult] = {
    // Ensure runMutant can always continue
    IO(results.applyOrElse(stream.next(), (_: Int) => results.head)())
      .recover { case _: ArrayIndexOutOfBoundsException => results.head() }
  }
}

object TestRunnerStub extends TestData {

  def resource = withResults(createMutant.toMutantResult(MutantStatus.Killed))

  def withResults(mutants: MutantResult*) = (_: Path) => makeResults(mutants)

  def withResults(initialTestRun: InitialTestRunResult)(mutants: MutantResult*) = (_: Path) =>
    makeResults(mutants, initialTestRun)

  def withInitialCompilerError(
      errs: NonEmptyList[CompilerErrMsg],
      mutants: MutantResult*
  ): Path => Either[NonEmptyList[CompilerErrMsg], Resource[IO, TestRunnerPool]] = {
    var firstRun = true
    (_: Path) =>
      if (firstRun) {
        firstRun = false
        errs.asLeft
      } else {
        makeResults(mutants)
      }
  }

  private def makeResults(
      mutants: Seq[MutantResult],
      initialTestRunResult: InitialTestRunResult = NoCoverageInitialTestRun(isSuccessful = true)
  ): Either[NonEmptyList[CompilerErrMsg], Resource[IO, TestRunnerPool]] =
    ResourcePool(
      NonEmptyList.one(
        Resource.pure[IO, TestRunner](new TestRunnerStub(mutants.map(() => _), initialTestRunResult))
      )
    ).asRight
}
