package stryker4s.files

import fs2.io.file.Path
import stryker4s.st.minimatch

class MinimatchFileMatcher(m: minimatch.mod.Minimatch) extends PathMatcher {
  override def matches(path: Path): Boolean = m.`match`(path.toString)
}
