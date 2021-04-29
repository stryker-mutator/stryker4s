package stryker4s.extension

import java.nio.file.Path

import better.files._
import stryker4s.config.Config

object FileExtensions {
  implicit class RelativePathExtension(file: File) {

    /** The path relative to the base-dir of the project.
      *
      * For example, with the file `projectRoot/src/main`, this function will return `src/main`
      */
    def relativePath(implicit config: Config): Path = file.path.relativePath

    /** The directory for this file, using `subDir` param as the base-directory instead of the Config base-dir.
      *
      * For example, with `this` file `projectRoot/src/main` folder, and the parameter file `projectRoot/target/tmp`,
      * this function will return projectRoot/target/tmp/src/main
      */
    def inSubDir(subDir: File)(implicit config: Config): File = file.path.inSubDir(subDir.path)
  }

  implicit class PathExtensions(path: Path) {

    /** The path relative to the base-dir of the project.
      *
      * For example, with the file `projectRoot/src/main`, this function will return `src/main`
      */
    def relativePath(implicit config: Config): Path = config.baseDir.path.relativize(path)

    /** The directory for this file, using `subDir` param as the base-directory instead of the Config base-dir.
      *
      * For example, with `this` file `projectRoot/src/main` folder, and the parameter file `projectRoot/target/tmp`,
      * this function will return projectRoot/target/tmp/src/main
      */
    def inSubDir(subDir: Path)(implicit config: Config): Path = subDir.resolve(path.relativePath.toString)
  }
}
