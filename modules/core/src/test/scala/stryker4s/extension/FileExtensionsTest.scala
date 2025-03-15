package stryker4s.extension

import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.extension.FileExtensions.*
import stryker4s.testkit.Stryker4sSuite

class FileExtensionsTest extends Stryker4sSuite {

  val absoluteBaseDir = Path(".").absolute
  implicit val config: Config = Config.default.copy(baseDir = absoluteBaseDir)

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

    test("should return the path if the given path is relative") {
      val result = Path("src/main").relativePath

      assertEquals(result, Path("src/main"))
    }

    test("works if both are relative") {
      val result = (Path("module") / "src" / "main").relativePath(Path("module"))
      assertEquals(result, Path("src") / "main")
    }

    test("works if both are absolute") {
      val result =
        (config.baseDir / "module" / "src" / "main").relativePath(config.baseDir / "module")
      assertEquals(result, Path("src/main"))
    }

    test("works if baseDir is relative and path is absolute") {
      val result = (absoluteBaseDir / "module" / "src" / "main").relativePath(Path("module"))
      assertEquals(result, Path("src/main"))
    }

    test("works if baseDir is absolute and path is relative") {
      val result = Path("module/src/main").relativePath(absoluteBaseDir / "module")
      assertEquals(result, Path("src/main"))
    }

    test("multiple calls doesn't change the path") {
      val result = Path("src/main").relativePath.relativePath.relativePath
      assertEquals(result, Path("src/main"))
    }
  }

  describe("inSubDir") {
    test("should calculate a path relative to the new subDir") {
      val baseDir = config.baseDir
      val sut = baseDir / "src" / "main"
      val subDir = baseDir / "target" / "tmp"

      val result = sut.inSubDir(subDir)

      assertEquals(result, Path("target/tmp/src/main").absolute)
    }
  }
}
