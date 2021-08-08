package stryker4s.extension

import fs2.io.file.Path
import stryker4s.config.Config

object FileExtensions {

  implicit class PathExtensions(path: Path) {

    /** The path relative to the base-dir of the project.
      *
      * For example, with the file `projectRoot/src/main`, this function will return `src/main`
      */
    def relativePath(implicit config: Config): Path = config.baseDir.relativize(path)

    /** The directory for this file, using `subDir` param as the base-directory instead of the Config base-dir.
      *
      * For example, with `this` file `projectRoot/src/main` folder, and the parameter file `projectRoot/target/tmp`,
      * this function will return projectRoot/target/tmp/src/main
      */
    def inSubDir(subDir: Path)(implicit config: Config): Path = subDir.resolve(path.relativePath.toString)
  }
}
