package stryker4s.mutants.findmutants

import stryker4s.Stryker4sSuite
import stryker4s.config.Config
import stryker4s.scalatest.FileUtil

class FileCollectorTest extends Stryker4sSuite {
  describe("collectFiles") {

    describe("on empty dir") {
      val emptyDir = FileUtil.getResource("fileTests/emptyDir")
      assume(emptyDir.exists(), "Empty test dir does not exist")

      it("should not collect the baseDir") {
        implicit val config: Config = Config(baseDir = emptyDir)
        val sut = new FileCollector()

        val results = sut.collectFiles()

        results should be(empty)
      }
    }

    describe("on filled dir") {
      val filledDirPath = FileUtil.getResource("fileTests/filledDir")

      assume(filledDirPath.exists(), "Filled test dir does not exist")

      it("should find all scala files and not the non-scala files with default config") {
        implicit val config: Config = Config(baseDir = filledDirPath)
        val sut = new FileCollector()

        val results = sut.collectFiles()

        results should have size 2
        val basePath = filledDirPath / "src/main/scala/package"
        results should contain only (basePath / "someFile.scala", basePath / "secondFile.scala")
      }

      it("should find matching files with custom config match pattern") {
        implicit val config: Config =
          Config(files = Seq("src/**/second*.scala"), baseDir = filledDirPath)
        val sut = new FileCollector()

        val results = sut.collectFiles()
        val onlyResult = results.loneElement

        onlyResult should equal(filledDirPath / "src/main/scala/package/secondFile.scala")
      }

      it("should find no matches with a non-matching glob") {
        implicit val config: Config =
          Config(files = Seq("**/noMatchesToBeFoundHere.scala"), baseDir = filledDirPath)
        val sut = new FileCollector()

        val results = sut.collectFiles()

        results should be(empty)
      }

      it("should match on multiple globs") {
        implicit val config: Config =
          Config(files = Seq("**/someFile.scala", "**/secondFile.scala"), baseDir = filledDirPath)
        val sut = new FileCollector()

        val results = sut.collectFiles()

        results should have size 2

        val basePath = filledDirPath / "src/main/scala/package"
        results should contain only (basePath / "someFile.scala", basePath / "secondFile.scala")
      }
    }
  }
}
