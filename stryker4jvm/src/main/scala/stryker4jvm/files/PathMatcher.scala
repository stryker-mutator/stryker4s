package stryker4jvm.files

import fs2.io.file.Path

/** `java.nio.file.PathMatcher`, but for `fs2.io.file.Path`
  */
trait PathMatcher {
  def matches(path: Path): Boolean
}
