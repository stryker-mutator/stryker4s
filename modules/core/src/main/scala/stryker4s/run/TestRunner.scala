package stryker4s.run

import cats.effect.{Deferred, IO, Ref, Resource}
import cats.syntax.option.*
import fansi.Color
import mutationtesting.{MutantResult, MutantStatus}
import stryker4s.config.Config
import stryker4s.extension.DurationExtensions.HumanReadableExtension
import stryker4s.extension.ResourceExtensions.*
import stryker4s.log.Logger
import stryker4s.model.{InitialTestRunResult, MutantWithId}
import stryker4s.testrunner.api.TestFile

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*

trait TestRunner {
  def initialTestRun(): IO[InitialTestRunResult]
  def runMutant(mutant: MutantWithId, testNames: Seq[TestFile]): IO[MutantResult]
}

/** Wrapping testrunners to add functionality to existing testrunners
  */
object TestRunner {

  def timeoutRunner(timeout: Deferred[IO, FiniteDuration], inner: Resource[IO, TestRunner])(implicit
      config: Config,
      log: Logger
  ): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (testRunnerF, releaseAndSwap) =>
      IO {
        new TestRunner {
          override def runMutant(mutant: MutantWithId, testNames: Seq[TestFile]): IO[MutantResult] =
            for {
              runner <- testRunnerF
              time <- timeout.get
              result <- runner
                .runMutant(mutant, testNames)
                .timeoutTo(
                  time,
                  IO(log.debug(s"Mutant ${mutant.id} timed out over ${time.toHumanReadable}")) *>
                    releaseAndSwap
                      .as(
                        mutant.toMutantResult(
                          MutantStatus.Timeout,
                          statusReason = s"Timeout of ${time.toHumanReadable} exceeded.".some
                        )
                      )
                ) <* IO.cede
            } yield result

          override def initialTestRun(): IO[InitialTestRunResult] =
            for {
              runner <- testRunnerF
              t <- runner.initialTestRun().timed
              (timedDuration, result) = t
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
    inner.selfRecreatingResource { (testRunnerF, releaseAndSwap) =>
      IO {
        new TestRunner {

          override def runMutant(mutant: MutantWithId, testNames: Seq[TestFile]): IO[MutantResult] =
            retryRunMutation(mutant, testNames)

          def retryRunMutation(
              mutant: MutantWithId,
              testNames: Seq[TestFile],
              retriesLeft: Long = 2
          ): IO[MutantResult] = {
            testRunnerF.flatMap(_.runMutant(mutant, testNames)).handleErrorWith { _ =>
              // On error, get a new testRunner and set it
              IO(
                log.debug(
                  s"Testrunner crashed for mutant ${mutant.id}. Starting a new one and retrying this mutant $retriesLeft more time(s)"
                )
              ) *>
                // Release old resource and make a new one, then retry the mutation
                releaseAndSwap *>
                (if (retriesLeft > 0) retryRunMutation(mutant, testNames, retriesLeft - 1)
                 else IO.pure(mutant.toMutantResult(MutantStatus.RuntimeError)))
            }
          }
          override def initialTestRun(): IO[InitialTestRunResult] =
            testRunnerF.flatMap(_.initialTestRun())
        }
      }
    }

  def maxReuseTestRunner(maxReuses: Int, inner: Resource[IO, TestRunner])(implicit
      log: Logger
  ): Resource[IO, TestRunner] =
    inner.selfRecreatingResource { (testRunnerF, releaseAndSwap) =>
      Ref[IO].of(0).map { usesRef =>
        new TestRunner {
          def runMutant(mutant: MutantWithId, testNames: Seq[TestFile]): IO[MutantResult] = for {
            uses <- usesRef.getAndUpdate(_ + 1)
            _ <-
              // If the limit has been reached, create a new testrunner
              IO.whenA(uses == maxReuses) {
                IO(log.info(s"Testrunner has run for $uses times. Restarting it...")) *>
                  releaseAndSwap *>
                  usesRef.set(1)
              }
            runner <- testRunnerF
            result <- runner.runMutant(mutant, testNames)
          } yield result

          override def initialTestRun(): IO[InitialTestRunResult] = for {
            _ <- usesRef.update(_ + 1)
            runner <- testRunnerF
            result <- runner.initialTestRun()
          } yield result
        }
      }
    }
}
