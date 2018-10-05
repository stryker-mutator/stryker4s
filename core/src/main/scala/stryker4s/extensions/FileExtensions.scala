package stryker4s.extensions
import java.nio.file.Path

import better.files.File
import stryker4s.config.Config
object FileExtensions {
  implicit class RelativePathExtension(file: File) {
    def relativePath(implicit config: Config): Path = config.baseDir.relativize(file)
  }
}
