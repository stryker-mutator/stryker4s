package stryker4s.files

import fs2.io.file.Path

import java.nio.file

class NioPathMatcher(matcher: file.PathMatcher) extends PathMatcher {
  override def matches(path: Path): Boolean = matcher.matches(path.toNioPath)
}
