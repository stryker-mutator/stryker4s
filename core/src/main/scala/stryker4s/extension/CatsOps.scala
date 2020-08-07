package stryker4s.extension

import cats.implicits._
import cats.effect.Sync
import cats.effect.Clock
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object CatsOps {
  implicit class Timed[F[_]: Sync, T](f: F[T])(implicit clock: Clock[F]) {

    private val timeUnit = TimeUnit.MILLISECONDS

    /** * Time how long an `F[_]` takes to execute
      *
      * @return a tuple of [[T]] and the FiniteDuration of the execution
      */
    def timed: F[(T, FiniteDuration)] =
      for {
        startTime <- Clock[F].realTime(timeUnit)
        result <- f
        endTime <- Clock[F].realTime(timeUnit)
      } yield (result, FiniteDuration(endTime - startTime, timeUnit))
  }
}
