package stryker4s.run

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._

import cats.effect.concurrent.MVar2
import cats.effect.{ContextShift, IO, Resource, Timer}
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.extension.CatsEffectExtensions._
import stryker4s.extension.ResourceExtensions._
import stryker4s.model.{Error, Mutant, MutantRunResult, TimedOut}

trait TestRunner {
  def initialTestRun(): IO[Boolean]
  def runMutant(mutant: Mutant): IO[MutantRunResult]
}

object TestRunner {

  def timeoutRunner(timeout: MVar2[IO, FiniteDuration], inner: Resource[IO, TestRunner])(implicit
      config: Config,
      timer: Timer[IO],
      cs: ContextShift[IO]
  ): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (mvar, releaseAndSwap) =>
      IO {
        new TestRunner with Logging {
          override def runMutant(mutant: Mutant): IO[MutantRunResult] =
            for {
              (runner, _) <- mvar.read
              time <- timeout.read
              result <- runner
                .runMutant(mutant)
                .timeoutTo(
                  time,
                  IO(debug(s"Mutant ${mutant.id} timed out over ${time.toCoarsest}")) *>
                    releaseAndSwap
                      .as(TimedOut(mutant))
                )
            } yield result

          override def initialTestRun(): IO[Boolean] =
            for {
              (runner, _) <- mvar.read
              (result, duration) <- runner.initialTestRun().timed
              newTimeout = calculateTimeout(duration)
              _ <- timeout.put(newTimeout)
              _ <- IO(info(s"Timeout set to ${newTimeout.toCoarsest} (net ${duration.toCoarsest})"))
            } yield result

          def calculateTimeout(netTimeMS: FiniteDuration)(implicit config: Config): FiniteDuration =
            FiniteDuration((netTimeMS.toMillis * config.timeoutFactor).toLong, TimeUnit.MILLISECONDS) + config.timeout
        }
      }
    }

  def retryRunner(inner: Resource[IO, TestRunner])(implicit
      cs: ContextShift[IO]
  ): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (mvar, releaseAndSwap) =>
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
