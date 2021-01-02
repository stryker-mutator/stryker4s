package stryker4s.testutil.stubs

import java.nio.file.Path

import scala.meta._

import cats.effect.{IO, Resource}
import stryker4s.extension.mutationtype.LesserThan
import stryker4s.model.{Killed, Mutant, MutantRunResult}
import stryker4s.run.TestRunner
import scala.collection.mutable.Queue

class TestRunnerStub(results: Seq[MutantRunResult]) extends TestRunner {
  val queue = Queue.from(results.toIterable)

  def initialTestRun(): IO[stryker4s.run.InitialTestRunResult] = IO.pure(Left(true))

  def runMutant(mutant: Mutant): IO[MutantRunResult] = {
    // Ensure runMutant can always continue
    results.headOption.foreach(queue.enqueue(_))
    IO.pure(queue.dequeue())
  }
}

object TestRunnerStub {

  def resource = withResults(Killed(Mutant(0, q">", q"<", LesserThan)))

  def withResults(mutants: MutantRunResult*) = (_: Path) => Resource.pure[IO, TestRunner](new TestRunnerStub(mutants))
}
