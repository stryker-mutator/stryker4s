package stryker4s.extensions

import fs2.io.file.Path
import stryker4jvm.config.Config

object FileExtensions {

  implicit final class PathExtensions(val path: Path) extends AnyVal {

    /** The path relative to the base-dir of the project.
      *
      * For example, with the file `projectRoot/src/main`, this function will return `src/main`
      */
    final def relativePath(implicit config: Config): Path = config.baseDir.relativize(path)

    /** The directory for this file, using `subDir` param as the base-directory instead of the Config base-dir.
      *
      * For example, with `this` file `projectRoot/src/main` folder, and the parameter file `projectRoot/target/tmp`,
      * this function will return projectRoot/target/tmp/src/main
      */
    final def inSubDir(subDir: Path)(implicit config: Config): Path = subDir.resolve(path.relativePath.toString)
  }
}
