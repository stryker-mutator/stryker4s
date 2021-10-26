package stryker4s.files

import fs2.io.file.Path
import stryker4s.st.minimatch

trait GlobPlatformCompat extends GlobPlatformCompatBase {

  override def matcher(path: Path, patterns: Seq[String]): PathMatcher = {
    val matchers = patterns.map(new minimatch.mod.Minimatch(_)).map(new MinimatchFileMatcher(_))

    new CompositePathMatcher(matchers)
  }

}
