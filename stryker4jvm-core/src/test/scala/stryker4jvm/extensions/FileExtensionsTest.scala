package stryker4jvm.extensions

import fs2.io.file.Path
import stryker4jvm.config.Config
import stryker4jvm.extensions.FileExtensions.PathExtensions
import stryker4jvm.testutil.Stryker4jvmSuite

class FileExtensionsTest extends Stryker4jvmSuite {

  implicit val config: Config = Config.default.copy(baseDir = Path("/home/project/stryker4s"))

  describe("relativePath") {
    it("should return the relative path on a file inside the base-dir") {
      val expectedRelativePath = Path("core/src/test/scala/stryker4s/extension/FileExtensions.scala")
      val sut = config.baseDir / expectedRelativePath

      val result = sut.relativePath

      result should equal(expectedRelativePath)
    }

    it("should return just the file name when a file is in the base-dir") {
      val expectedRelativePath = Path("build.sbt")
      val sut = config.baseDir / expectedRelativePath

      val result = sut.relativePath

      result should equal(expectedRelativePath)
    }
  }

  describe("inSubDir") {
    it("should calculate a path relative to the new subDir") {
      val baseDir = config.baseDir
      val sut = baseDir / "src" / "main"
      val subDir = baseDir / "target" / "tmp"

      val result = sut.inSubDir(subDir)

      result should equal(Path("/home/project/stryker4s/target/tmp/src/main"))
    }
  }
}
