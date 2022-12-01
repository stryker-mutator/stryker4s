package stryker4jvm.files

import cats.effect.IO
import fs2.io.file.Path

sealed trait FileResolver {
  def files: fs2.Stream[IO, Path]
}

/** Resolve files that can be be mutated
  */
trait MutatesFileResolver extends FileResolver

/** Resolve files that need to be copied over to the temp directory (includes source files that are not mutated)
  */
trait FilesFileResolver extends FileResolver
