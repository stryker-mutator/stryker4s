package stryker4s.run

import cats.data.{NonEmptyList, NonEmptyVector}
import cats.effect.IO
import fs2.io.file.{Files, Path}
import mutationtesting.MutantStatus
import stryker4s.config.Config
import stryker4s.model.*
import stryker4s.report.Reporter
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.stubs.{TestFileResolver, TestRunnerStub}
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite, TestData}

import scala.meta.*

class MutantRunnerTest extends Stryker4sIOSuite with MockitoIOSuite with LogMatchers with TestData {

  describe("apply") {
    val baseDir = FileUtil.getResource("scalaFiles")
    val staticTmpDir = baseDir.resolve("target/stryker4s-tmpDir")

    implicit val config = Config.default.copy(baseDir = baseDir)

    val staticTmpDirConfig = config.copy(staticTmpDir = true)
    val noCleanTmpDirConfig = staticTmpDirConfig.copy(cleanTmpDir = false)

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[Reporter]
      val rollbackHandler = mock[RollbackHandler]
      when(reporterMock.mutantTested).thenReturn(_.drain)
      val mutant = createMutant.copy(id = MutantId(3))
      val secondMutant = createMutant.copy(id = MutantId(1))
      val thirdMutant = createMutant.copy(id = MutantId(2))

      val testRunner = { (path: Path) =>
        // Static temp dir is not used with default settings.
        path.toString should not endWith "stryker4s-tmpDir"
        TestRunnerStub.withResults(
          mutant.toMutantResult(MutantStatus.Killed),
          secondMutant.toMutantResult(MutantStatus.Killed),
          thirdMutant.toMutantResult(MutantStatus.Survived)
        )(path)
      }

      val sut = new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = NonEmptyVector.of(mutant, secondMutant, thirdMutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants)
      sut(Seq(mutatedFile)).asserting { case RunResult(results, _) =>
        "Setting up mutated environment..." shouldBe loggedAsInfo
        "Starting initial test run..." shouldBe loggedAsInfo
        "Initial test run succeeded! Testing mutants..." shouldBe loggedAsInfo
        val (path, resultForFile) = results.loneElement
        path shouldBe file
        resultForFile.map(_.status) shouldBe List(MutantStatus.Killed, MutantStatus.Survived, MutantStatus.Killed)
      }
    }

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed and 1 doesn't compile.") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[Reporter]
      val rollbackHandler = mock[RollbackHandler]
      when(reporterMock.mutantTested).thenReturn(_.drain)
      val mutant = createMutant.copy(id = MutantId(3))
      val secondMutant = createMutant.copy(id = MutantId(1))
      val thirdMutant = createMutant.copy(id = MutantId(2))
      val compileErrorResult = thirdMutant.toMutantResult(MutantStatus.CompileError)

      val testRunner =
        TestRunnerStub.withInitialCompilerError(
          NonEmptyList.one(CompilerErrMsg("blah", "scalaFiles/simpleFile.scala", 123)),
          mutant.toMutantResult(MutantStatus.Killed),
          secondMutant.toMutantResult(MutantStatus.Killed)
        )

      val sut = new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = NonEmptyVector.of(mutant, secondMutant, thirdMutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants)
      when(rollbackHandler.rollbackFiles(any[NonEmptyList[CompilerErrMsg]], any[Vector[MutatedFile]])).thenReturn(
        Right(
          RollbackResult(
            Vector(mutatedFile.copy(mutants = NonEmptyVector.of(mutant, secondMutant))),
            Map(file -> Vector(compileErrorResult))
          )
        )
      )
      sut(Vector(mutatedFile)).asserting { case RunResult(results, _) =>
        val (path, resultForFile) = results.loneElement
        path shouldBe file
        "Attempting to remove 1 mutant(s) that gave a compile error..." shouldBe loggedAsInfo
        resultForFile.map(_.status) shouldBe List(MutantStatus.Killed, MutantStatus.Killed, MutantStatus.CompileError)
      }
    }

    it("should use static temp dir if it was requested") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[Reporter]
      val rollbackHandler = mock[RollbackHandler]
      when(reporterMock.mutantTested).thenReturn(_.drain)
      val mutant = createMutant.copy(id = MutantId(3))

      val testRunner = { (path: Path) =>
        // Static temp dir is used.
        path.toString should endWith("stryker4s-tmpDir")
        TestRunnerStub.withResults(mutant.toMutantResult(MutantStatus.Killed))(path)
      }

      val sut =
        new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)(staticTmpDirConfig, testLogger)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = NonEmptyVector.one(mutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants)

      sut(Seq(mutatedFile)).asserting { _ =>
        // Cleaned up after run
        staticTmpDir.toNioPath.toFile shouldNot exist
      }
    }

    it("should not clean up tmp dir on errors") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[Reporter]
      val rollbackHandler = mock[RollbackHandler]

      val testRunner = TestRunnerStub.withResults(initialTestRunResultIsSuccessful = false)()
      val mutant = createMutant.copy(id = MutantId(3))

      val sut =
        new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)(staticTmpDirConfig, testLogger)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = NonEmptyVector.one(mutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants)

      sut(Seq(mutatedFile)).attempt
        .asserting { result =>
          staticTmpDir.toNioPath.toFile should exist

          result shouldBe a[Left[Throwable, ?]]
          result.asInstanceOf[Left[Throwable, ?]].value.getMessage should startWith("Initial test run failed")
        }
        .flatMap { result =>
          // Simulate the user manually cleaned up the tmp dir (before we run the next test case).
          Files[IO].deleteRecursively(staticTmpDir).as(result)
        }
    }

    it("should not clean up tmp dir if clean-tmp-dir is disabled") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[Reporter]
      val rollbackHandler = mock[RollbackHandler]

      when(reporterMock.mutantTested).thenReturn(_.drain)
      val mutant = createMutant.copy(id = MutantId(1))

      val testRunner = TestRunnerStub.withResults(mutant.toMutantResult(MutantStatus.Killed))

      val sut =
        new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)(noCleanTmpDirConfig, testLogger)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = NonEmptyVector.one(mutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants)

      sut(Seq(mutatedFile))
        .asserting { _ =>
          staticTmpDir.toNioPath.toFile should exist
        }
        .flatMap { result =>
          // Simulate the user manually cleaned up the tmp dir (before we run the next test case).
          Files[IO].deleteRecursively(staticTmpDir).as(result)
        }
    }
  }
}
