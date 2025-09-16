package stryker4s.extension

import cats.effect.std.NonEmptyHotswap
import cats.effect.{Concurrent, Resource}
import cats.syntax.applicative.*
import cats.syntax.option.*

object ResourceExtensions {

  implicit final class SelfRecreatingResource[F[_], A](val startResource: Resource[F, A]) extends AnyVal {

    /** Build a resource that can destroy and recreate the 'inner' resource by evaluating a passed `F[Unit]`. The inner
      * resource value is available inside a thread-safe mutable `Ref`
      *
      * @param f
      *   Map function with two parameters:
      *   - The value of the resource in a `F[A]`
      *   - A `F[Unit]` that releases the 'old' Resource and recreates a new one to store in the `F[A]`
      * @return
      *   The new resource created with @param f
      */
    final def selfRecreatingResource[B](f: (F[A], F[Unit]) => F[B])(implicit F: Concurrent[F]): Resource[F, B] = {
      val next = startResource.map(_.some)
      NonEmptyHotswap(next).evalMap { hotswap =>
        // .clear to run the finalizer of the existing resource first before starting a new one
        val releaseAndSwapF = F.guarantee(hotswap.clear, hotswap.swap(next))
        f(hotswap.getOpt.use(_.get.pure[F]), releaseAndSwapF)
      }
    }
  }
}
