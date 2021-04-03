package stryker4s.run

import cats.effect._
import cats.effect.std._
import fs2.Stream
import fs2.Pull
import stryker4s.model.Mutant
class TestRunnerPool(testRunners: Stream[IO, TestRunner]) {

  /** Get a value from the pool and evaluate the effect. Afterwards, the `T` is put back in the pool
    *
    * @param f
    * @return
    */
  def run(mutants: Queue[IO, Mutant]) = {

    testRunners.pull.peek1
      .flatMap {
        case None => Pull.done
        case Some((testRunner, _)) =>
          Pull.eval(mutants.take.flatMap(testRunner.runMutant(_)))
      }
      .flatMap(Pull.output1(_))
      .stream
  }
  //  mutants. Stream.m take.flatMap { t =>
  //   Sync[IO].guarantee(f(t), testRunners.offer(t))
  // }
}

object TestRunnerPool {
  def create[T](testRunners: Stream[IO, T]): Resource[IO, TestRunnerPool] = {
    ???
    // Queue.unbounded[IO, T].flatMap { queue =>
    //     Semaphore[IO](8).flatMap { semaphore =>
    //       val push = testRunners.parEvalMap(8)(queue.offer(_))

    //       val permits = semaphore.permit.evalMap(_ => queue.take)

    //     }
    //   }
    // }
  }
}
