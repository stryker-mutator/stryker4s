package stryker4s.mutants.findmutants

import better.files.File
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchers._
import stryker4s.Stryker4sSuite
import stryker4s.config.Config
import stryker4s.run.process.{Command, ProcessRunner}
import stryker4s.scalatest.{FileUtil, LogMatchers}

import scala.util.{Failure, Try}

class FileCollectorTest extends Stryker4sSuite with MockitoSugar with LogMatchers {

  private val filledDirPath: File = FileUtil.getResource("fileTests/filledDir")
  private val basePath: File = filledDirPath / "src/main/scala/package"

  assume(filledDirPath.exists(), "Filled test dir does not exist")
  assume(basePath.exists(), "Basepath dir does not exist")

  describe("collect files to mutate") {
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

  describe("Collect files to copy over to tmp folder") {

    val processRunnerMock: ProcessRunner = mock[ProcessRunner]

    it("Should execute git process to collect files"){
      implicit val config: Config = Config(baseDir = filledDirPath)
      val filePath = "Config.scala"
      val expectedFileList = Seq(config.baseDir / filePath)
      val gitProcessResult = Try(Seq(filePath))
      when(processRunnerMock(Command("git ls-files", "--others --exclude-standard --cached"), config.baseDir)).thenReturn(gitProcessResult)

      val sut = new FileCollector()

      val results = sut.filesToCopy(processRunnerMock)

      results should contain theSameElementsAs expectedFileList
    }

    it("Should copy over files with target in their name") {
      implicit val config: Config = Config(baseDir = filledDirPath)
      val filePath = "Target.scala"
      val expectedFileList = Seq(config.baseDir / filePath)
      val gitProcessResult = Try(Seq(filePath))
      when(processRunnerMock(Command("git ls-files", "--others --exclude-standard --cached"), config.baseDir)).thenReturn(gitProcessResult)

      val sut = new FileCollector()

      val results = sut.filesToCopy(processRunnerMock)

      results should contain theSameElementsAs expectedFileList
    }

    it("Should copy the files from the files config key when the target repo is not a git repo") {
      implicit val config: Config = Config(baseDir = filledDirPath, files = Some(Seq("**/main/scala/**/*.scala")))
      val expectedFileList = Seq(basePath / "someFile.scala", basePath / "secondFile.scala")
      val gitProcessResult = Failure(new Exception("Exception"))
      when(processRunnerMock(any[Command], any[File])).thenReturn(gitProcessResult)

      val sut = new FileCollector()

      val results = sut.filesToCopy(processRunnerMock)

      results should contain theSameElementsAs expectedFileList
    }

    it("Should copy not copy files our of the target folders when no files config key is found and target repo is not a git repo") {
      implicit val config: Config = Config(baseDir = filledDirPath, files = None)
      val gitProcessResult = Failure(new Exception("Exception"))
      when(processRunnerMock(any[Command], any[File])).thenReturn(gitProcessResult)

      val sut = new FileCollector()

      val results = sut.filesToCopy(processRunnerMock)

      // Is always 0 because there is target in the folder path for tests.
      results should have size 0
    }

    describe("log tests"){
      it("Should log that no git repo is found and is using fallback") {
        implicit val config: Config = Config(baseDir = filledDirPath)
        val gitProcessResult = Failure(new Exception(""))
        when(processRunnerMock(any[Command], any[File])).thenReturn(gitProcessResult)

        val sut = new FileCollector()

        sut.filesToCopy(processRunnerMock)

        "Not a git repo, falling back to 'files' configuration." shouldBe loggedAsInfo
      }

      it("Should log that no files config option is found and is using fallback to copy all files") {
        implicit val config: Config = Config(baseDir = filledDirPath)
        val gitProcessResult = Failure(new Exception(""))
        when(processRunnerMock(any[Command], any[File])).thenReturn(gitProcessResult)

        val sut = new FileCollector()

        sut.filesToCopy(processRunnerMock)

        "No 'files' specified, falling back to copying everything except the target/ folder(s)" shouldBe loggedAsWarning
      }
    }
  }
}
