package stryker4s.extensions
import java.nio.file.Path

import stryker4s.config.Config
object PathExtensions {
  implicit class RelativePathExtension(path: Path) {
    def relative(implicit config: Config): Path = config.baseDir.relativize(path)
  }
}
