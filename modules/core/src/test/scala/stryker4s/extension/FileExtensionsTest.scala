package stryker4s.extension

import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.extension.FileExtensions.*
import stryker4s.testkit.Stryker4sSuite

class FileExtensionsTest extends Stryker4sSuite {

  implicit val config: Config = Config.default.copy(baseDir = Path("/home/project/stryker4s"))

  describe("relativePath") {
    test("should return the relative path on a file inside the base-dir") {
      val expectedRelativePath = Path("core/src/test/scala/stryker4s/extension/FileExtensions.scala")
      val sut = config.baseDir / expectedRelativePath

      val result = sut.relativePath

      assertEquals(result, expectedRelativePath)
    }

    test("should return just the file name when a file is in the base-dir") {
      val expectedRelativePath = Path("build.sbt")
      val sut = config.baseDir / expectedRelativePath

      val result = sut.relativePath

      assertEquals(result, expectedRelativePath)
    }
  }

  describe("inSubDir") {
    test("should calculate a path relative to the new subDir") {
      val baseDir = config.baseDir
      val sut = baseDir / "src" / "main"
      val subDir = baseDir / "target" / "tmp"

      val result = sut.inSubDir(subDir)

      assertEquals(result, Path("/home/project/stryker4s/target/tmp/src/main"))
    }
  }
}
