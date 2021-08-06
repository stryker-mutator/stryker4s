package stryker4s.testutil.stubs

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.applicativeError._
import stryker4s.extension.mutationtype.LesserThan
import stryker4s.model.{InitialTestRunResult, Killed, Mutant, MutantRunResult, NoCoverageInitialTestRun}
import stryker4s.run.{ResourcePool, TestRunner}

import java.nio.file.Path
import scala.meta._

class TestRunnerStub(results: Seq[() => MutantRunResult]) extends TestRunner {
  private[this] val stream = Iterator.from(0)

  def initialTestRun(): IO[InitialTestRunResult] = IO.pure(NoCoverageInitialTestRun(true))

  def runMutant(mutant: Mutant): IO[MutantRunResult] = {
    // Ensure runMutant can always continue
    IO(results.applyOrElse(stream.next(), (_: Int) => results.head)())
      .recover { case _: ArrayIndexOutOfBoundsException => results.head() }
  }
}

object TestRunnerStub {

  def resource = withResults(Killed(Mutant(0, q">", q"<", LesserThan)))

  def withResults(mutants: MutantRunResult*) = (_: Path) =>
    ResourcePool(NonEmptyList.of(Resource.pure[IO, TestRunner](new TestRunnerStub(mutants.map(() => _)))))
}
