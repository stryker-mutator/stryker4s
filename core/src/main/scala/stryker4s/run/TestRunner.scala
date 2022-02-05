package stryker4s.run

import cats.effect.{Deferred, IO, Ref, Resource}
import fansi.Color
import stryker4s.config.Config
import stryker4s.extension.DurationExtensions.HumanReadableExtension
import stryker4s.extension.ResourceExtensions.*
import stryker4s.log.Logger
import stryker4s.model.{Error, InitialTestRunResult, Mutant, MutantRunResult, TimedOut}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*

trait TestRunner {
  def initialTestRun(): IO[InitialTestRunResult]
  def runMutant(mutant: Mutant, testNames: Seq[String]): IO[MutantRunResult]
}

/** Wrapping testrunners to add functionality to existing testrunners
  */
object TestRunner {

  def timeoutRunner(timeout: Deferred[IO, FiniteDuration], inner: Resource[IO, TestRunner])(implicit
      config: Config,
      log: Logger
  ): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (testRunnerRef, releaseAndSwap) =>
      IO {
        new TestRunner {
          override def runMutant(mutant: Mutant, testNames: Seq[String]): IO[MutantRunResult] =
            for {
              runner <- testRunnerRef.get
              time <- timeout.get
              result <- runner
                .runMutant(mutant, testNames)
                .timeoutTo(
                  time,
                  IO(log.debug(s"Mutant ${mutant.id} timed out over ${time.toHumanReadable}")) *>
                    releaseAndSwap
                      .as(TimedOut(mutant))
                )
            } yield result

          override def initialTestRun(): IO[InitialTestRunResult] =
            for {
              runner <- testRunnerRef.get
              (timedDuration, result) <- runner.initialTestRun().timed
              // Use reported duration if its available, or timed duration as a backup
              duration = result.reportedDuration.getOrElse(timedDuration)

              setTimeout = calculateTimeout(duration)
              isSet <- timeout.complete(setTimeout)
              _ <-
                IO.whenA(isSet) {
                  IO(log.info(s"Timeout set to ${setTimeout.toHumanReadable} (${Color
                      .LightGray(s"net ${duration.toHumanReadable}")})"))
                }
            } yield result

          def calculateTimeout(netTimeMS: FiniteDuration)(implicit config: Config): FiniteDuration =
            FiniteDuration((netTimeMS.toMillis * config.timeoutFactor).toLong, TimeUnit.MILLISECONDS) + config.timeout
        }
      }
    }

  def retryRunner(
      inner: Resource[IO, TestRunner]
  )(implicit log: Logger): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (testRunnerRef, releaseAndSwap) =>
      IO {
        new TestRunner {

          override def runMutant(mutant: Mutant, testNames: Seq[String]): IO[MutantRunResult] =
            retryRunMutation(mutant, testNames)

          def retryRunMutation(
              mutant: Mutant,
              testNames: Seq[String],
              retriesLeft: Long = 2
          ): IO[MutantRunResult] = {
            testRunnerRef.get.flatMap(_.runMutant(mutant, testNames)).handleErrorWith { _ =>
              // On error, get a new testRunner and set it
              IO(
                log.debug(
                  s"Testrunner crashed for mutant ${mutant.id}. Starting a new one and retrying this mutant $retriesLeft more time(s)"
                )
              ) *>
                // Release old resource and make a new one, then retry the mutation
                releaseAndSwap *>
                (if (retriesLeft > 0) retryRunMutation(mutant, testNames, retriesLeft - 1)
                 else IO.pure(Error(mutant)))
            }
          }
          override def initialTestRun(): IO[InitialTestRunResult] =
            testRunnerRef.get.flatMap(_.initialTestRun())
        }
      }
    }

  def maxReuseTestRunner(maxReuses: Int, inner: Resource[IO, TestRunner])(implicit
      log: Logger
  ): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (testRunnerRef, releaseAndSwap) =>
      Ref[IO].of(0).map { usesRef =>
        new TestRunner {
          def runMutant(mutant: Mutant, testNames: Seq[String]): IO[MutantRunResult] = for {
            uses <- usesRef.getAndUpdate(_ + 1)
            _ <-
              // If the limit has been reached, create a new testrunner
              IO.whenA(uses == maxReuses) {
                IO(log.info(s"Testrunner has run for $uses times. Restarting it...")) *>
                  releaseAndSwap *>
                  usesRef.set(1)
              }
            runner <- testRunnerRef.get
            result <- runner.runMutant(mutant, testNames)
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
