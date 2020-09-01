package stryker4s.run

import scala.concurrent.duration._

import cats.effect.{ContextShift, IO, Resource, Timer}
import grizzled.slf4j.Logging
import stryker4s.extension.ResourceExtensions
import stryker4s.model.{Error, Mutant, MutantRunResult, TimedOut}

trait TestRunner {
  def initialTestRun(): IO[Boolean]
  def runMutant(mutant: Mutant): IO[MutantRunResult]
}

object TestRunner {

  def timeoutRunner(timeout: FiniteDuration, inner: Resource[IO, TestRunner])(implicit
      timer: Timer[IO],
      cs: ContextShift[IO]
  ): Resource[IO, TestRunner] =
    ResourceExtensions.selfRecreatingResource(inner) { (mvar, releaseAndSwap) =>
      IO {
        new TestRunner with Logging {
          override def runMutant(mutant: Mutant): IO[MutantRunResult] =
            mvar.read
              .flatMap(_._1.runMutant(mutant))
              .timeoutTo(
                timeout,
                IO(debug(s"Mutant ${mutant.id} timed out over ${timeout.toCoarsest}")) *>
                  releaseAndSwap *>
                  IO.pure(
                    TimedOut(mutant)
                  )
              )

          override def initialTestRun(): IO[Boolean] =
            mvar.read.flatMap(_._1.initialTestRun())
        }
      }
    }

  def retryRunner(acquire: Resource[IO, TestRunner])(implicit
      cs: ContextShift[IO]
  ): Resource[IO, TestRunner] =
    ResourceExtensions.selfRecreatingResource(acquire) { (mvar, releaseAndSwap) =>
      IO {
        new TestRunner with Logging {

          override def runMutant(mutant: Mutant): IO[MutantRunResult] =
            retryRunMutation(mutant)

          def retryRunMutation(mutant: Mutant, retriesLeft: Long = 2): IO[MutantRunResult] = {
            mvar.read.flatMap(_._1.runMutant(mutant)).attempt.flatMap {
              // On error, get a new testRunner and set it
              case Left(_) =>
                IO(
                  info(
                    s"TestRunner crashed for mutant ${mutant.id}. Starting a new one and retrying this mutant ${retriesLeft} more time(s)"
                  )
                ) *>
                  // Release old resource and make a new one, then retry the mutation
                  releaseAndSwap *>
                  (if (retriesLeft > 0) retryRunMutation(mutant, retriesLeft - 1)
                   else IO.pure(Error(mutant)))

              case Right(value) => IO.pure(value)
            }
          }
          override def initialTestRun(): IO[Boolean] =
            mvar.read.flatMap(_._1.initialTestRun())

        }
      }
    }
}
