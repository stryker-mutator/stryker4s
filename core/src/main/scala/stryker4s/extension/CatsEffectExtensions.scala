package stryker4s.extension

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import cats.effect.{Clock, Sync}
import cats.implicits._

object CatsEffectExtensions {

  implicit class Timed[F[_], T](f: F[T]) {

    private val timeUnit = TimeUnit.MILLISECONDS

    /** * Time how long an `F[_]` takes to execute
      *
      * @return a tuple of `T` and the FiniteDuration of the execution
      */
    def timed(implicit F: Sync[F], clock: Clock[F]): F[(T, FiniteDuration)] =
      for {
        startTime <- Clock[F].realTime(timeUnit)
        result <- f
        endTime <- Clock[F].realTime(timeUnit)
      } yield (result, FiniteDuration(endTime - startTime, timeUnit))
  }
}
