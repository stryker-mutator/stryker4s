package stryker4s.extensions
import java.nio.file.Paths

import better.files._
import stryker4s.Stryker4sSuite
import stryker4s.config.Config
import stryker4s.extensions.PathExtensions._

class PathExtensionsTest extends Stryker4sSuite {
  describe("relativePath") {
    implicit val config: Config = Config()

    it("should return the relative path on a file inside the base-dir") {
      val expectedRelativePath =
        Paths.get("util/src/test/scala/stryker4s/extensions/FileExtensions.scala")
      val sut = File.currentWorkingDirectory / expectedRelativePath.toString

      val result = sut.path.relative

      result should equal(expectedRelativePath)
    }

    it("should return just the file name when a file is in the base-dir") {
      val expectedRelativePath = Paths.get("build.sbt")
      val sut = File.currentWorkingDirectory / expectedRelativePath.toString

      val result = sut.path.relative

      result should equal(expectedRelativePath)
    }
  }
}
