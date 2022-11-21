package stryker4jvm.testutil.stubs

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.applicativeError.*
import fs2.io.file.Path
import mutationtesting.{MutantResult, MutantStatus}

class TestRunnerStub(results: Seq[() => MutantResult]) extends TestRunner {
  private val stream = Iterator.from(0)

  def initialTestRun(): IO[InitialTestRunResult] = IO.pure(NoCoverageInitialTestRun(true))

  def runMutant(mutant: MutantWithId, testNames: Seq[String]): IO[MutantResult] = {
    // Ensure runMutant can always continue
    IO(results.applyOrElse(stream.next(), (_: Int) => results.head)())
      .recover { case _: ArrayIndexOutOfBoundsException => results.head() }
  }
}

object TestRunnerStub extends TestData {

  def resource = withResults(createMutant.toMutantResult(MutantStatus.Killed))

  def withResults(mutants: MutantResult*) = (_: Path) => makeResults(mutants)

  def withInitialCompilerError(
      errs: NonEmptyList[CompilerErrMsg],
      mutants: MutantResult*
  ): Path => Either[NonEmptyList[CompilerErrMsg], Resource[IO, TestRunnerPool]] = {
    var firstRun = true
    (_: Path) =>
      if (firstRun) {
        firstRun = false
        Left(errs)
      } else {
        makeResults(mutants)
      }
  }

  private def makeResults(
      mutants: Seq[MutantResult]
  ): Either[NonEmptyList[CompilerErrMsg], Resource[IO, TestRunnerPool]] =
    ResourcePool(NonEmptyList.one(Resource.pure[IO, TestRunner](new TestRunnerStub(mutants.map(() => _))))).asRight
}