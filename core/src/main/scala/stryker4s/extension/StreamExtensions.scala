package stryker4s.extension

import fs2.Stream

object StreamExtensions {
  @SuppressWarnings(Array("stryker4s.mutation.MethodExpression"))
  implicit class FilterNotExtension[F[_], O](s: Stream[F, O]) {
    def filterNot(p: O => Boolean): Stream[F, O] = s.mapChunks(_.filter(!p(_)))
  }
}
