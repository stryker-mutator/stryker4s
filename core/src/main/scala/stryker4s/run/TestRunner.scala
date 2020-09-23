package stryker4s.run

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._

import cats.effect.concurrent.{Deferred, Ref}
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

  def timeoutRunner(timeout: Deferred[IO, FiniteDuration], inner: Resource[IO, TestRunner])(implicit
      config: Config,
      timer: Timer[IO],
      cs: ContextShift[IO]
  ): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (testRunnerRef, releaseAndSwap) =>
      IO {
        new TestRunner with Logging {
          override def runMutant(mutant: Mutant): IO[MutantRunResult] =
            for {
              runner <- testRunnerRef.get
              time <- timeout.get
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
              runner <- testRunnerRef.get
              (result, duration) <- runner.initialTestRun().timed
              newTimeout = calculateTimeout(duration)
              _ <- timeout.complete(newTimeout)
              _ <- IO(info(s"Timeout set to ${newTimeout.toCoarsest} (net ${duration.toCoarsest})"))
            } yield result

          def calculateTimeout(netTimeMS: FiniteDuration)(implicit config: Config): FiniteDuration =
            FiniteDuration((netTimeMS.toMillis * config.timeoutFactor).toLong, TimeUnit.MILLISECONDS) + config.timeout
        }
      }
    }

  def retryRunner(inner: Resource[IO, TestRunner]): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (testRunnerRef, releaseAndSwap) =>
      IO {
        new TestRunner with Logging {

          override def runMutant(mutant: Mutant): IO[MutantRunResult] =
            retryRunMutation(mutant)

          def retryRunMutation(mutant: Mutant, retriesLeft: Long = 2): IO[MutantRunResult] = {
            testRunnerRef.get.flatMap(_.runMutant(mutant)).attempt.flatMap {
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
            testRunnerRef.get.flatMap(_.initialTestRun())
        }
      }
    }

  def maxReuseTestRunner(maxReuses: Int, inner: Resource[IO, TestRunner]): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (testRunnerRef, releaseAndSwap) =>
      Ref[IO].of(0).map { usesRef =>
        new TestRunner with Logging {
          def runMutant(mutant: Mutant): IO[MutantRunResult] = for {
            uses <- usesRef.getAndUpdate(_ + 1)
            _ <-
              // If the limit has been reached, create a new testrunner
              if (uses >= maxReuses)
                IO(info(s"Testrunner has run for $uses times. Restarting it...")) *>
                  releaseAndSwap *>
                  usesRef.set(1)
              else IO.unit
            runner <- testRunnerRef.get
            result <- runner.runMutant(mutant)
          } yield result

          override def initialTestRun(): IO[Boolean] =
            testRunnerRef.get.flatMap(_.initialTestRun())
        }
      }
    }
}
