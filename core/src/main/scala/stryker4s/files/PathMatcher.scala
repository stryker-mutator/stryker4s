package stryker4s.files

import fs2.io.file.Path

/** `java.nio.file.PathMatcher`, but for `fs2.io.file.Path`
  */
trait PathMatcher {
  def matches(path: Path): Boolean
}

class CompositePathMatcher(private val matchers: Seq[PathMatcher]) extends PathMatcher {
  override def matches(path: Path): Boolean = matchers.exists(_.matches(path))
}

class InvertedPathMatcher(matcher: PathMatcher) extends PathMatcher {
  override def matches(path: Path): Boolean = !matcher.matches(path)
}
