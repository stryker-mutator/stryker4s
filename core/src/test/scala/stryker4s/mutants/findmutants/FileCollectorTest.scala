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

        val results = sut.collectFilesToMutate()

        results should be(empty)
      }
    }

    describe("on filled dir") {
      val filledDirPath = FileUtil.getResource("fileTests/filledDir")
      val basePath = filledDirPath / "src/main/scala/package"

      assume(filledDirPath.exists(), "Filled test dir does not exist")
      assume(basePath.exists(), "Basepath dir does not exist")

      it("should find all scala files and not the non-scala files with default config") {
        implicit val config: Config = Config(baseDir = filledDirPath)
        val sut = new FileCollector()

        val results = sut.collectFilesToMutate()

        results should have size 2
        results should contain only (basePath / "someFile.scala", basePath / "secondFile.scala")
      }

      it("should find matching files with custom config match pattern") {
        implicit val config: Config =
          Config(mutate = Seq("src/**/second*.scala"), baseDir = filledDirPath)
        val sut = new FileCollector()

        val results = sut.collectFilesToMutate()
        val onlyResult = results.loneElement

        onlyResult should equal(basePath / "secondFile.scala")
      }

      it("should find no matches with a non-matching glob") {
        implicit val config: Config =
          Config(mutate = Seq("**/noMatchesToBeFoundHere.scala"), baseDir = filledDirPath)
        val sut = new FileCollector()

        val results = sut.collectFilesToMutate()

        results should be(empty)
      }

      it("should match on multiple globs") {
        implicit val config: Config =
          Config(mutate = Seq("**/someFile.scala", "**/secondFile.scala"), baseDir = filledDirPath)
        val sut = new FileCollector()

        val results = sut.collectFilesToMutate()

        results should have size 2
        results should contain only (basePath / "someFile.scala", basePath / "secondFile.scala")
      }

      it("should only add a glob once even when it matches twice") {
        implicit val config: Config =
          Config(mutate = Seq("**/someFile.scala", "**/*.scala"), baseDir = filledDirPath)
        val sut = new FileCollector()

        val results = sut.collectFilesToMutate()

        results should have size 2
        results should contain only (basePath / "someFile.scala", basePath / "secondFile.scala")
      }

      it("should not find a file twice when the patterns match on the same file twice") {
        implicit val config: Config = Config(mutate = Seq("**/someFile.scala",
                                                          "**/secondFile.scala",
                                                          "!**/*.scala",
                                                          "!**/someFile.scala"),
                                             baseDir = filledDirPath)

        val sut = new FileCollector()

        val results = sut.collectFilesToMutate()

        results should be(empty)
      }

      it("Should exclude the file specified in the excluded files config") {
        implicit val config: Config = Config(
          mutate = Seq("**/someFile.scala", "**/secondFile.scala", "!**/someFile.scala"),
          baseDir = filledDirPath
        )

        val sut = new FileCollector()

        val results = sut.collectFilesToMutate()

        results should have size 1
        results should contain only (basePath / "secondFile.scala")
      }

      it("Should exclude all files specified in the excluded files config") {
        implicit val config: Config = Config(mutate = Seq("**/someFile.scala",
                                                          "**/secondFile.scala",
                                                          "!**/someFile.scala",
                                                          "!**/secondFile.scala"),
                                             baseDir = filledDirPath)

        val sut = new FileCollector()

        val results = sut.collectFilesToMutate()

        results should be(empty)
      }

      it("Should exclude all files based on a wildcard") {
        implicit val config: Config = Config(
          mutate = Seq("**/someFile.scala", "**/secondFile.scala", "!**/*.scala"),
          baseDir = filledDirPath
        )

        val sut = new FileCollector()

        val results = sut.collectFilesToMutate()

        results should be(empty)
      }

      it("Should exclude all files from previous runs in the target folder") {
        implicit val config: Config = Config(baseDir = filledDirPath)

        val sut = new FileCollector()

        val results = sut.collectFilesToMutate()

        results should have size 2
        results should contain only (basePath / "someFile.scala", basePath / "secondFile.scala")
      }

      it("Should not exclude a non existing file") {
        implicit val config: Config = Config(
          mutate = Seq("**/someFile.scala", "**/secondFile.scala", "!**/nonExistingFile.scala"),
          baseDir = filledDirPath
        )

        val sut = new FileCollector()

        val results = sut.collectFilesToMutate()

        results should have size 2
        results should contain only (basePath / "someFile.scala", basePath / "secondFile.scala")
      }
    }
  }
}
