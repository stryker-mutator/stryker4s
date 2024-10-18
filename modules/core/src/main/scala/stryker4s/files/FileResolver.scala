package stryker4s.files

import cats.effect.IO
import fs2.io.file.Path

trait FileResolver {
  def files: fs2.Stream[IO, Path]
}
