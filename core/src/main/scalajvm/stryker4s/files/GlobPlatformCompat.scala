package stryker4s.files

import fs2.io.file.Path

import java.nio.file

trait GlobPlatformCompat extends GlobPlatformCompatBase {

  override def matcher(path: Path, patterns: Seq[String]): PathMatcher = {
    val (ignorePatterns, validPatterns) = patterns.partition(_.startsWith("!"))
    val ignorePatternsStripped = ignorePatterns.map(_.stripPrefix("!"))

    val separator = java.io.File.separatorChar

    // Escape start of the path so it is not parsed as part of the glob expression, but as a literal
    val escapedPath = (path.toString + separator)
      .replace("\\", "\\\\")
      .replace("*", "\\*")
      .replace("?", "\\?")
      .replace("{", "\\{")
      .replace("}", "\\}")
      .replace("[", "\\[")
      .replace("]", "\\]")

    def toPathMatcher(glob: String): PathMatcher = new NioPathMatcher(
      path.toNioPath.getFileSystem().getPathMatcher(s"glob:$escapedPath$glob")
    )

    val matchers = validPatterns.map(toPathMatcher)
    val ignoreMatchers =
      ignorePatternsStripped.map(toPathMatcher).map(new InvertedPathMatcher(_))

    new CompositePathMatcher(matchers ++ ignoreMatchers)
  }
}
