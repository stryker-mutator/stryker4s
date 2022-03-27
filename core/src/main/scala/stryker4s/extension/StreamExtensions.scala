package stryker4s.extension

import fs2.Stream

object StreamExtensions {
  @SuppressWarnings(Array("stryker4s.mutation.MethodExpression"))
  implicit final class FilterNotExtension[F[_], O](val s: Stream[F, O]) extends AnyVal {
    final def filterNot(p: O => Boolean): Stream[F, O] = s.filter(!p(_))
  }
}
