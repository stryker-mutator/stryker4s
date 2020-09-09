package stryker4s.extension

import cats.effect.concurrent.{MVar, MVar2}
import cats.effect.{Concurrent, Resource}
import cats.syntax.all._

object ResourceExtensions {
  implicit class SelfRecreatingResource[F[_], A](startResource: Resource[F, A]) {

    /** Build a resource that can destroy and recreate 'itself' by evaluating a passed `F[Unit]`. The resource value is available inside a thread-safe mutable `MVar`
      *
      * @param f Map function with two parameters:
      *   - The value of the resource in a `MVar`
      *   - A `F[Unit]` that releases the 'old' Resource and recreates a new one to store in the `MVar`
      * @return The new resource
      */
    def selfRecreatingResource(
        f: (MVar2[F, (A, F[Unit])], F[Unit]) => F[A]
    )(implicit F: Concurrent[F]): Resource[F, A] = {

      val allocatedF: F[(A, F[Unit])] = startResource.allocated.flatMap { allocated =>
        MVar.of[F, (A, F[Unit])](allocated).flatMap { mvar =>
          // Release old and start a new Resource
          val releaseAndSwap: F[Unit] = mvar.modify_ { case (_, release) =>
            release *>
              startResource.allocated
          }
          f(mvar, releaseAndSwap).map(left => (left, mvar.read.flatMap(_._2)))
        }
      }
      Resource(allocatedF)
    }
  }
}
