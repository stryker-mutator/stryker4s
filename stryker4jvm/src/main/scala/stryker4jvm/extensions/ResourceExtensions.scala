package stryker4jvm.extensions

import cats.effect.std.Hotswap
import cats.effect.{Concurrent, Ref, Resource}
import cats.syntax.flatMap.*

object ResourceExtensions {

  implicit final class SelfRecreatingResource[F[_], A](val startResource: Resource[F, A]) extends AnyVal {

    /** Build a resource that can destroy and recreate the 'inner' resource by evaluating a passed `F[Unit]`. The inner
      * resource value is available inside a thread-safe mutable `Ref`
      *
      * @param f
      *   Map function with two parameters:
      *   - The value of the resource in a `Ref`
      *   - A `F[Unit]` that releases the 'old' Resource and recreates a new one to store in the `Ref`
      * @return
      *   The new resource created with @param f
      */
    final def selfRecreatingResource[B](f: (Ref[F, A], F[Unit]) => F[B])(implicit F: Concurrent[F]): Resource[F, B] =
      Hotswap(startResource).evalMap { case (hotswap, r) =>
        Ref[F].of(r).flatMap { ref =>
          // .clear to run the finalizer of the existing resource first before starting a new one
          val releaseAndSwapF = F.guarantee(hotswap.clear, hotswap.swap(startResource).flatMap(ref.set(_)))
          f(ref, releaseAndSwapF)
        }
      }
  }
}
