package stryker4s.extension

import fs2.Stream

object StreamExtensions {
  @SuppressWarnings(Array("stryker4s.mutation.MethodExpression"))
  implicit class FilterNotExtension[F[_], O](val s: Stream[F, O]) extends AnyVal {
    def filterNot(p: O => Boolean): Stream[F, O] = s.filter(!p(_))
  }
}
