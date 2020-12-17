package stryker4s.run

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._

import cats.effect.concurrent.{Deferred, Ref}
import cats.effect.{ContextShift, IO, Resource, Timer}
import stryker4s.config.Config
import stryker4s.extension.CatsEffectExtensions._
import stryker4s.extension.ResourceExtensions._
import stryker4s.log.Logger
import stryker4s.model.{Error, Mutant, MutantRunResult, TimedOut}

trait TestRunner {
  def initialTestRun(): IO[InitialTestRunResult]
  def runMutant(mutant: Mutant): IO[MutantRunResult]
}

object TestRunner {

  def timeoutRunner(timeout: Deferred[IO, FiniteDuration], inner: Resource[IO, TestRunner])(implicit
      config: Config,
      log: Logger,
      timer: Timer[IO],
      cs: ContextShift[IO]
  ): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (testRunnerRef, releaseAndSwap) =>
      IO {
        new TestRunner {
          override def runMutant(mutant: Mutant): IO[MutantRunResult] =
            for {
              runner <- testRunnerRef.get
              time <- timeout.get
              result <- runner
                .runMutant(mutant)
                .timeoutTo(
                  time,
                  IO(log.debug(s"Mutant ${mutant.id} timed out over ${time.toCoarsest}")) *>
                    releaseAndSwap
                      .as(TimedOut(mutant))
                )
            } yield result

          override def initialTestRun(): IO[InitialTestRunResult] =
            for {
              runner <- testRunnerRef.get
              (result, duration) <- runner.initialTestRun().timed
              newTimeout = calculateTimeout(duration)
              _ <-
                if (result.fold(identity, _.isSuccessful))
                  timeout.complete(newTimeout) *>
                    IO(log.info(s"Timeout set to ${newTimeout.toCoarsest} (net ${duration.toCoarsest})"))
                else IO.unit
            } yield result

          def calculateTimeout(netTimeMS: FiniteDuration)(implicit config: Config): FiniteDuration =
            FiniteDuration((netTimeMS.toMillis * config.timeoutFactor).toLong, TimeUnit.MILLISECONDS) + config.timeout
        }
      }
    }

  def retryRunner(
      inner: Resource[IO, TestRunner]
  )(implicit log: Logger, cs: ContextShift[IO]): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (testRunnerRef, releaseAndSwap) =>
      IO {
        new TestRunner {

          override def runMutant(mutant: Mutant): IO[MutantRunResult] =
            retryRunMutation(mutant)

          def retryRunMutation(mutant: Mutant, retriesLeft: Long = 2): IO[MutantRunResult] = {
            testRunnerRef.get.flatMap(_.runMutant(mutant)).attempt.flatMap {
              // On error, get a new testRunner and set it
              case Left(_) =>
                IO(
                  log.info(
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
          override def initialTestRun(): IO[InitialTestRunResult] =
            testRunnerRef.get.flatMap(_.initialTestRun())
        }
      }
    }

  def maxReuseTestRunner(maxReuses: Int, inner: Resource[IO, TestRunner])(implicit
      log: Logger,
      cs: ContextShift[IO]
  ): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (testRunnerRef, releaseAndSwap) =>
      Ref[IO].of(0).map { usesRef =>
        new TestRunner {
          def runMutant(mutant: Mutant): IO[MutantRunResult] = for {
            uses <- usesRef.getAndUpdate(_ + 1)
            _ <-
              // If the limit has been reached, create a new testrunner
              if (uses >= maxReuses)
                IO(log.info(s"Testrunner has run for $uses times. Restarting it...")) *>
                  releaseAndSwap *>
                  usesRef.set(1)
              else IO.unit
            runner <- testRunnerRef.get
            result <- runner.runMutant(mutant)
          } yield result

          override def initialTestRun(): IO[InitialTestRunResult] = for {
            _ <- usesRef.update(_ + 1)
            runner <- testRunnerRef.get
            result <- runner.initialTestRun()
          } yield result
        }
      }
    }
}
