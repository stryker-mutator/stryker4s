package stryker4s.extension

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import cats.effect.concurrent.{MVar, MVar2}
import cats.effect.{Clock, Concurrent, Resource, Sync}
import cats.implicits._

object CatsEffectOps {

  implicit class Timed[F[_]: Sync, T](f: F[T]) {

    private val timeUnit = TimeUnit.MILLISECONDS

    /** * Time how long an `F[_]` takes to execute
      *
      * @return a tuple of `T` and the FiniteDuration of the execution
      */
    def timed(implicit clock: Clock[F]): F[(T, FiniteDuration)] =
      for {
        startTime <- Clock[F].realTime(timeUnit)
        result <- f
        endTime <- Clock[F].realTime(timeUnit)
      } yield (result, FiniteDuration(endTime - startTime, timeUnit))
  }

  /** Build a resource that can destroy and recreate 'itself' with a passed function. The resource value is available inside a thread-safe mutable `MVar`
    *
    * @param startResource `F` to create a resource
    * @param f Map function with two parameters:
    *   - The value of the resource in a `MVar`
    *   - A `F[Unit]` that releases the 'old' Resource and recreates a new one in the `MVar`
    * @return The resource
    */
  def selfRecreatingResource[F[_]: Concurrent, A](
      startResource: Resource[F, A]
  )(f: (MVar2[F, (A, F[Unit])], F[Unit]) => F[A]): Resource[F, A] = {
    import cats.implicits._

    val allocatedF: F[(A, F[Unit])] = startResource.allocated.flatMap { allocated =>
      MVar.of[F, (A, F[Unit])](allocated).flatMap { mvar =>
        // Release old and start a new Resource
        val releaseAndSwap: F[Unit] = mvar.modify_ {
          case (_, release) =>
            release *>
              startResource.allocated
        }
        f(mvar, releaseAndSwap).map(left => (left, mvar.read.flatMap(_._2)))
      }
    }
    Resource(allocatedF)
  }
}
