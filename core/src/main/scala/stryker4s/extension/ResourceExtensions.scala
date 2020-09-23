package stryker4s.extension

import cats.effect.concurrent.Ref
import cats.effect.{Resource, Sync}
import cats.syntax.all._

object ResourceExtensions {

  implicit class SelfRecreatingResource[F[_], A <: AnyRef](startResource: Resource[F, A]) {

    /** Build a resource that can destroy and recreate the 'inner' resource by evaluating a passed `F[Unit]`. The inner resource value is available inside a thread-safe mutable `Ref`
      *
      * @param f Map function with two parameters:
      *   - The value of the resource in a `Ref`
      *   - A `F[Unit]` that releases the 'old' Resource and recreates a new one to store in the `Ref`
      * @return The new resource created with @param f
      */
    def selfRecreatingResource(f: (Ref[F, A], F[Unit]) => F[A])(implicit F: Sync[F]): Resource[F, A] = {
      val allocatedF = for {
        innerState <- startResource.allocated.flatMap(Ref.of(_))

        releaseAndSwapF = innerState.get.flatMap(_._2) *> // Release old
          startResource.allocated.flatMap(innerState.set(_)) // Set new

        // lens of the `A` in the Ref to pass to the builder function
        refOfA = Ref.lens(innerState)(_._1, r => (a: A) => (a, r._2))

        newA <- f(refOfA, releaseAndSwapF)
        innerRelease = innerState.get.flatMap(_._2)
      } yield (newA, innerRelease)

      Resource(allocatedF)
    }
  }
}
