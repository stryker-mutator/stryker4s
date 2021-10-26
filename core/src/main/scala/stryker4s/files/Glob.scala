package stryker4s.files

import fs2.io.file.Path
import cats.effect.IO
import fs2.io.file.Files
import stryker4s.extension.StreamExtensions._

object Glob extends GlobPlatformCompat {

  def glob(path: Path, list: Seq[String]): fs2.Stream[IO, Path] = {
    val matchers = matcher(path, list)
    Files[IO]
      .walk(path)
      .filterNot(!matchers.matches(_))
      .evalFilterNot(Files[IO].isDirectory)
  }
}

trait GlobPlatformCompatBase {

  /** Create a PathMatcher for a given path that matches on the given glob patterns.
    *
    * @param path
    *   the path to match
    * @param patterns
    *   the glob patterns to match on, any pattern starting with `!` will be a negative match
    */
  def matcher(path: Path, patterns: Seq[String]): PathMatcher

}
