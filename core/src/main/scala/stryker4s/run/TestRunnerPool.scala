package stryker4s.run

import cats.data.NonEmptyList
import cats.effect._
import cats.effect.std._
import cats.syntax.all._
import fs2.Stream
import stryker4s.log.Logger
import stryker4s.model.{Mutant, MutantRunResult}
import stryker4s.report.{Progress, StartMutationEvent}

import java.nio.file.Path

/** @param testRunnerLoan Take 1 TestRunner from the pool. Puts the testrunner back into the pool when the resource closes
  */
class TestRunnerPool private (val testRunnerLoan: Resource[IO, TestRunner])(implicit log: Logger) {

  /** Run all given mutants on the testrunner pool in parallel
    */
  def run(
      mutants: List[(Path, Mutant)],
      onStart: StartMutationEvent => IO[Unit]
  ): Stream[IO, (Path, MutantRunResult)] = {
    val totalTestableMutants = mutants.size

    val runMutantsOnPool = mutants.zipWithIndex.parTraverse { case ((path, mutant), index) =>
      testRunnerLoan.use { tr =>
        IO(log.debug(s"Running mutant $mutant")) *>
          onStart(StartMutationEvent(Progress(index + 1, totalTestableMutants))) *>
          tr.runMutant(mutant).tupleLeft(path)
      }
    }
    Stream.eval(runMutantsOnPool).flatMap(Stream.emits(_))
  }

}

object TestRunnerPool {

  def apply(testRunners: Resource[IO, NonEmptyList[TestRunner]])(implicit log: Logger): Resource[IO, TestRunnerPool] =
    Resource.eval(Queue.unbounded[IO, TestRunner]).flatMap { queue =>
      val publish = testRunners.evalMap(_.parTraverse_(queue.offer))

      def testRunnerLoan = Resource.make(IO(log.debug("Taking testrunner from pool")) *> queue.take)(tr =>
        IO(log.debug("Putting testrunner back into pool")) *> queue.offer(tr)
      )

      publish.as(new TestRunnerPool(testRunnerLoan))
    }
}
