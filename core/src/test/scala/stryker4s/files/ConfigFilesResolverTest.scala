package stryker4s.files

import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.run.process.{Command, ProcessRunner}
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite}

import scala.util.{Failure, Try}

class ConfigFilesResolverTest extends Stryker4sIOSuite with MockitoIOSuite with LogMatchers {
  describe("files") {

    val filledDirPath: Path = FileUtil.getResource("fileTests/filledDir")
    val basePath: Path = filledDirPath / "src/main/scala"

    it("Should execute git process to collect files") {
      implicit val config: Config = Config.default.copy(baseDir = filledDirPath)
      val processRunnerMock: ProcessRunner = mock[ProcessRunner]
      val filePath = "src/main/scala/package/someFile.scala"
      val expectedFileList = Seq(config.baseDir / filePath)
      val gitProcessResult = Try(Seq(filePath))
      when(processRunnerMock(Command("git ls-files", "--others --exclude-standard --cached"), config.baseDir))
        .thenReturn(gitProcessResult)

      val sut = new ConfigFilesResolver(processRunnerMock)

      sut.files.compile.toVector.asserting {
        _ should contain theSameElementsAs expectedFileList
      }
    }

    it("Should copy over files with target in their name") {
      implicit val config: Config = Config.default.copy(baseDir = filledDirPath)
      val processRunnerMock: ProcessRunner = mock[ProcessRunner]
      val filePath = "src/main/scala/package/target.scala"
      val expectedFileList = Seq(config.baseDir / filePath)
      val gitProcessResult = Try(Seq(filePath))
      when(processRunnerMock(Command("git ls-files", "--others --exclude-standard --cached"), config.baseDir))
        .thenReturn(gitProcessResult)

      val sut = new ConfigFilesResolver(processRunnerMock)

      sut.files.compile.toVector
        .asserting(
          _ should contain theSameElementsAs expectedFileList
        )
    }

    it("Should copy the files from the files config key") {
      implicit val config: Config =
        Config.default.copy(baseDir = filledDirPath, files = Seq("**/main/scala/**/*.scala"))
      val processRunnerMock: ProcessRunner = mock[ProcessRunner]
      val expectedFileList = Seq(
        basePath / "package" / "someFile.scala",
        basePath / "package" / "secondFile.scala",
        basePath / "package" / "target.scala"
      )

      val sut = new ConfigFilesResolver(processRunnerMock)

      sut.files.compile.toVector
        .asserting(
          _ should contain theSameElementsAs expectedFileList
        )
    }

    it(
      "Should copy files out of the target folders when no files config key is found and target repo is not a git repo"
    ) {
      implicit val config: Config = Config.default.copy(baseDir = basePath / "package", files = Seq.empty)
      val processRunnerMock: ProcessRunner = mock[ProcessRunner]
      val expectedFileList =
        Seq(
          basePath / "package" / "someFile.scala",
          basePath / "package" / "secondFile.scala",
          basePath / "package" / "otherFile.notScala",
          basePath / "package" / "target.scala"
        )
      val gitProcessResult = Failure(new Exception("Exception"))
      when(processRunnerMock(any[Command], any[Path])).thenReturn(gitProcessResult)

      val sut = new ConfigFilesResolver(processRunnerMock)

      sut.files.compile.toVector.asserting { results =>
        results should have size 4
        results should contain theSameElementsAs expectedFileList
      }
    }

    it("should filter out files that don't exist") {
      implicit val config: Config = Config.default.copy(baseDir = filledDirPath)
      val processRunnerMock: ProcessRunner = mock[ProcessRunner]
      val filePath = "src/main/scala/package/doesnotexist.scala"
      val gitProcessResult = Try(Seq(filePath))
      when(processRunnerMock(Command("git ls-files", "--others --exclude-standard --cached"), config.baseDir))
        .thenReturn(gitProcessResult)

      val sut = new ConfigFilesResolver(processRunnerMock)

      sut.files.compile.toVector.asserting {
        _ should be(empty)
      }
    }

    describe("log tests") {
      it("Should log that no files config option is found and is using fallback to copy all files") {
        implicit val config: Config = Config.default.copy(baseDir = filledDirPath)
        val processRunnerMock: ProcessRunner = mock[ProcessRunner]
        val gitProcessResult = Failure(new Exception(""))
        when(processRunnerMock(any[Command], any[Path])).thenReturn(gitProcessResult)

        val sut = new ConfigFilesResolver(processRunnerMock)

        sut.files.compile.drain.asserting { _ =>
          "No 'files' specified and not a git repository." shouldBe loggedAsWarning
          "Falling back to copying everything except the 'target/' folder(s)" shouldBe loggedAsWarning
        }
      }
    }
  }
}
