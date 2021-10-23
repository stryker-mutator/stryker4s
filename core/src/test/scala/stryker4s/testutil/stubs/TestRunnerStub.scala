package stryker4s.testutil.stubs

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.applicativeError._
import fs2.io.file.Path
import stryker4s.extension.mutationtype.LesserThan
import stryker4s.model._
import stryker4s.run.{ResourcePool, TestRunner, TestRunnerPool}

import scala.meta._

class TestRunnerStub(results: Seq[() => MutantRunResult]) extends TestRunner {
  private val stream = Iterator.from(0)

  def initialTestRun(): IO[InitialTestRunResult] = IO.pure(NoCoverageInitialTestRun(true))

  def runMutant(mutant: Mutant): IO[MutantRunResult] = {
    // Ensure runMutant can always continue
    IO(results.applyOrElse(stream.next(), (_: Int) => results.head)())
      .recover { case _: ArrayIndexOutOfBoundsException => results.head() }
  }
}

object TestRunnerStub {

  def resource = withResults(Killed(Mutant(MutantId(0), q">", q"<", LesserThan)))

  def withResults(mutants: MutantRunResult*) = (_: Path) => makeResults(mutants)

  def withInitialCompilerError(
      errs: List[CompilerErrMsg],
      mutants: MutantRunResult*
  ): Path => Either[NonEmptyList[CompilerErrMsg], Resource[IO, TestRunnerPool]] = {
    var firstRun = true
    (_: Path) =>
      if (firstRun) {
        firstRun = false
        Left(NonEmptyList.fromListUnsafe(errs))
      } else {
        makeResults(mutants)
      }
  }

  private def makeResults(
      mutants: Seq[MutantRunResult]
  ): Either[NonEmptyList[CompilerErrMsg], Resource[IO, TestRunnerPool]] =
    Right(ResourcePool(NonEmptyList.of(Resource.pure[IO, TestRunner](new TestRunnerStub(mutants.map(() => _))))))
}
