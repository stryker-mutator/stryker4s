package stryker4s.extension

import fs2.io.file.Path
import stryker4s.config.Config

import java.io.File

object FileExtensions {

  implicit final class PathExtensions(val path: Path) extends AnyVal {

    /** The path relative to the base-dir of the project.
      *
      * For example, with the file `projectRoot/src/main`, this function will return `src/main`
      */
    final def relativePath(implicit config: Config): Path = relativePath(config.baseDir)

    final def relativePath(base: Path) = (base.isAbsolute, path.isAbsolute) match {
      case (true, false) => base.relativize(path.absolute)
      case (false, true) => base.absolute.relativize(path)
      case (true, true)  => base.relativize(path)
      case (false, false) =>
        Path(path.toString.stripPrefix(base.toString + File.separator))
    }

    /** The directory for this file, using `subDir` param as the base-directory instead of the Config base-dir.
      *
      * For example, with `this` file `projectRoot/src/main` folder, and the parameter file `projectRoot/target/tmp`,
      * this function will return projectRoot/target/tmp/src/main
      */
    final def inSubDir(subDir: Path)(implicit config: Config): Path = subDir.resolve(path.relativePath.toString)
  }
}
