package stryker4s.sbt

import cats.effect.IO
import stryker4s.model.Mutant
import stryker4s.model.MutantRunResult
import cats.effect.Resource
import fs2.Stream
import scala.concurrent.duration._
import cats.effect.Timer
import cats.effect.ContextShift
import stryker4s.model.TimedOut

package object runner {
  type SbtTestRunner = Mutant => IO[MutantRunResult]

  def timeoutRunner(timeout: FiniteDuration, runner: SbtTestRunner)(implicit
      timer: Timer[IO],
      cs: ContextShift[IO]
  ): SbtTestRunner =
    mutant =>
      runner(mutant)
        .timeoutTo(timeout, IO.pure(TimedOut(mutant, null))) // TODO: path

  def retryRunner(acquire: Resource[IO, SbtTestRunner], maxAttempts: Long = 2)(implicit timer: Timer[IO]) =
    Stream
      .resource(acquire)
      .attempts(Stream.constant(50.millis)) // Wait 50 ms between retries
      .take(maxAttempts) // At most try maxAttempt times
      .takeThrough(_.isLeft) // Keep trying until succesful
      .last // Only get the last success
      .map(_.get)
      .rethrow

}
