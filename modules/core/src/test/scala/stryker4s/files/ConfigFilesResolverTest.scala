package stryker4s.files

import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.run.process.ProcessRunner
import stryker4s.testkit.{FileUtil, LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.stubs.TestProcessRunner

import scala.util.{Failure, Try}

class ConfigFilesResolverTest extends Stryker4sIOSuite with LogMatchers {
  describe("files") {

    val filledDirPath: Path = FileUtil.getResource("fileTests/filledDir")
    val basePath: Path = filledDirPath / "src/main/scala"

    test("should execute git process to collect files") {
      implicit val config: Config = Config.default.copy(baseDir = filledDirPath)
      val filePath = "src/main/scala/package/someFile.scala"
      val expectedFileList = Vector(config.baseDir / filePath)
      val gitProcessResult = Try(Seq(filePath))
      val processRunnerStub: ProcessRunner = TestProcessRunner(gitProcessResult)

      val sut = new ConfigFilesResolver(processRunnerStub)

      sut.files.compile.toVector.assertSameElementsAs(expectedFileList)
    }

    test("should copy over files with target in their name") {
      implicit val config: Config = Config.default.copy(baseDir = filledDirPath)
      val filePath = "src/main/scala/package/target.scala"
      val expectedFileList = Vector(config.baseDir / filePath)
      val gitProcessResult = Try(Seq(filePath))
      val processRunnerStub: ProcessRunner = TestProcessRunner(gitProcessResult)

      val sut = new ConfigFilesResolver(processRunnerStub)

      sut.files.compile.toVector
        .assertSameElementsAs(expectedFileList)
    }

    test("should copy the files from the files config key") {
      implicit val config: Config =
        Config.default.copy(baseDir = filledDirPath, files = Seq("**/main/scala/**/*.scala"))
      val processRunnerStub: ProcessRunner = TestProcessRunner()
      val expectedFileList = Vector(
        basePath / "package" / "someFile.scala",
        basePath / "package" / "target.scala",
        basePath / "package" / "secondFile.scala"
      )

      val sut = new ConfigFilesResolver(processRunnerStub)

      sut.files.compile.toVector
        .assertSameElementsAs(expectedFileList)
    }

    test(
      "Should copy files out of the target folders when no files config key is found and target repo is not a git repo"
    ) {
      implicit val config: Config = Config.default.copy(baseDir = basePath / "package", files = Seq.empty)
      val expectedFileList =
        Vector(
          basePath / "package" / "someFile.scala",
          basePath / "package" / "secondFile.scala",
          basePath / "package" / "otherFile.notScala",
          basePath / "package" / "target.scala"
        )
      val gitProcessResult: Try[Seq[String]] = Failure(new Exception("Exception"))
      val processRunnerStub: ProcessRunner = TestProcessRunner(gitProcessResult)

      val sut = new ConfigFilesResolver(processRunnerStub)

      sut.files.compile.toVector
        .assertSameElementsAs(expectedFileList)
    }

    test("should filter out files that don't exist") {
      implicit val config: Config = Config.default.copy(baseDir = filledDirPath)
      val filePath = "src/main/scala/package/doesnotexist.scala"
      val gitProcessResult = Try(Seq(filePath))
      val processRunnerStub: ProcessRunner = TestProcessRunner(gitProcessResult)

      val sut = new ConfigFilesResolver(processRunnerStub)

      sut.files.compile.toVector.assertEquals(Vector.empty)
    }

    describe("log tests") {
      test("should log that no files config option is found and is using fallback to copy all files") {
        implicit val config: Config = Config.default.copy(baseDir = filledDirPath)
        val gitProcessResult: Try[Seq[String]] = Failure(new Exception(""))
        val processRunnerStub: ProcessRunner = TestProcessRunner(gitProcessResult)

        val sut = new ConfigFilesResolver(processRunnerStub)

        sut.files.compile.drain.asserting { _ =>
          assertLoggedWarn("No 'files' specified and not a git repository.")
          assertLoggedWarn("Falling back to copying everything except the 'target/' folder(s)")
        }
      }
    }
  }
}
