package stryker4s.files

import fs2.io.file.Path
import cats.effect.IO
import fs2.io.file.Files
import java.nio.file
import stryker4s.extension.StreamExtensions.*

object Glob {

  /** Create a PathMatcher for a given path that matches on the given glob patterns.
    *
    * @param path
    *   the path to match
    * @param patterns
    *   the glob patterns to match on, any pattern starting with `!` will be a negative match
    */
  def matcher(path: Path, patterns: Seq[String]): PathMatcher = {
    val (ignorePatterns, validPatterns) = patterns.partition(_.startsWith("!"))
    val ignorePatternsStripped = ignorePatterns.map(_.stripPrefix("!"))

    val separator = path.toNioPath.getFileSystem().getSeparator()

    // Escape start of the path so it is not parsed as part of the glob expression, but as a literal
    val escapedPath = (path.toString + separator)
      .replace("\\", "\\\\")
      .replace("*", "\\*")
      .replace("?", "\\?")
      .replace("{", "\\{")
      .replace("}", "\\}")
      .replace("[", "\\[")
      .replace("]", "\\]")

    def toPathMatcher(glob: String): file.PathMatcher =
      path.toNioPath.getFileSystem().getPathMatcher(s"glob:$escapedPath$glob")

    val matchers = validPatterns.map(toPathMatcher)
    val ignoreMatchers = ignorePatternsStripped.map(toPathMatcher)

    new PathMatcher {
      def matches(path: Path): Boolean =
        matchers.exists(_.matches(path.toNioPath)) && !ignoreMatchers.exists(_.matches(path.toNioPath))
    }
  }

  def glob(path: Path, list: Seq[String]): fs2.Stream[IO, Path] = {
    val matcher = Glob.matcher(path, list)
    Files[IO]
      .walk(path)
      .filterNot(!matcher.matches(_))
      .evalFilterNot(Files[IO].isDirectory)
  }
}
