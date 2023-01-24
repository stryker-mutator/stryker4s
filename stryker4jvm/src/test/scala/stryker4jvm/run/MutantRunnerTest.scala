package stryker4jvm.run

import cats.data.{NonEmptyList, NonEmptyVector}
import cats.effect.IO
import fs2.io.file.{Files, Path}
import mutationtesting.MutantStatus
import org.mockito.captor.ArgCaptor
import stryker4jvm.config.Config
import stryker4jvm.core.model.MutantWithId
import stryker4jvm.extensions.MutantExtensions.ToMutantResultExtension
import stryker4jvm.model.{CompilerErrMsg, MutatedFile, RunResult}
import stryker4jvm.reporting.{FinishedRunEvent, IOReporter}
import stryker4jvm.scalatest.{FileUtil, LogMatchers}
import stryker4jvm.testutil.{MockAST, MockitoIOSuite, Stryker4jvmIOSuite, TestData}
import stryker4jvm.testutil.stubs.{TestFileResolver, TestRunnerStub}

import scala.meta.*

class MutantRunnerTest extends Stryker4jvmIOSuite with MockitoIOSuite with LogMatchers with TestData {

  describe("apply") {
    val baseDir = FileUtil.getResource("mockFiles")
    val staticTmpDir = baseDir.resolve("target/stryker4jvm-tmpDir")

    implicit val config: Config = Config.default.copy(baseDir = baseDir)

    val staticTmpDirConfig = config.copy(staticTmpDir = true)
    val noCleanTmpDirConfig = staticTmpDirConfig.copy(cleanTmpDir = false)

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[IOReporter]
      val rollbackHandler = mock[RollbackHandler]
      when(reporterMock.mutantTested).thenReturn(_.drain)
      val mutant = new MutantWithId(3, createMutant.mutatedCode)
      val secondMutant = new MutantWithId(1, createMutant.mutatedCode)
      val thirdMutant = new MutantWithId(2, createMutant.mutatedCode)

      val testRunner = { (path: Path) =>
        // Static temp dir is not used with default settings.
        path.toString should not endWith "stryker4jvm-tmpDir"
        TestRunnerStub.withResults(
          mutant.toMutantResult(MutantStatus.Killed),
          secondMutant.toMutantResult(MutantStatus.Killed),
          thirdMutant.toMutantResult(MutantStatus.Survived)
        )(path)
      }

      val sut = new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)
      val file = FileUtil.getResource("mockFiles/simple.test")
      val mutants = Vector(mutant, secondMutant, thirdMutant)
      val mutatedFile = MutatedFile(file, new MockAST(""), mutants)
      sut(Seq(mutatedFile)).asserting { case RunResult(results, _) =>
        "Setting up mutated environment..." shouldBe loggedAsInfo
        "Starting initial test run..." shouldBe loggedAsInfo
        "Initial test run succeeded! Testing mutants..." shouldBe loggedAsInfo
        val (path, resultForFile) = results.loneElement
        path shouldBe file
        resultForFile.map(_.status) shouldBe List(MutantStatus.Killed, MutantStatus.Survived, MutantStatus.Killed)
      }
    }

//   it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed and 1 doesn't compile.") {
//     val fileCollectorMock = new TestFileResolver(Seq.empty)
//     val reporterMock = mock[IOReporter]
//     val rollbackHandler = mock[RollbackHandler]
//     when(reporterMock.mutantTested).thenReturn(_.drain)
//
//     val mutant = new MutantWithId(3, createMutant.mutatedCode)
//     val secondMutant = new MutantWithId(1, createMutant.mutatedCode)
//     val thirdMutant = new MutantWithId(2, createMutant.mutatedCode)
//     val nonCompilingMutant = new MutantWithId(4, createMutant.mutatedCode)
//
//     val errs = NonEmptyList.one(CompilerErrMsg("blah", "xyz", 123))
//     val testRunner =
//       TestRunnerStub.withInitialCompilerError(
//         errs,
//         mutant.toMutantResult(MutantStatus.Killed),
//         secondMutant.toMutantResult(MutantStatus.Killed),
//         thirdMutant.toMutantResult(MutantStatus.Survived),
//         nonCompilingMutant.toMutantResult(MutantStatus.CompileError)
//       )
//
//     val sut = new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)
//     val file = FileUtil.getResource("mockFiles/simple.test")
//     val mutants = Vector(mutant, secondMutant, thirdMutant)
//     val mutatedFile = MutatedFile(file, new MockAST(""), mutants)
//
//     sut(Seq(mutatedFile)).asserting { case RunResult(results, _) =>
//       val captor = ArgCaptor[FinishedRunEvent]
//       val runReport = captor.value.report.files.loneElement
//
//       "Setting up mutated environment..." shouldBe loggedAsInfo
//       "Starting initial test run..." shouldBe loggedAsInfo
//       "Initial test run succeeded! Testing mutants..." shouldBe loggedAsInfo
//       "Attempting to remove mutants that gave a compile error..." shouldBe loggedAsInfo
//
//       runReport._1 shouldBe "simple.test"
//       runReport._2.mutants.map(_.id) shouldBe List("1", "2", "3", "4")
//       val (path, resultForFile) = results.loneElement
//
//       resultForFile.mutationScore shouldBe ((2d / 3d) * 100)
//       result.totalMutants shouldBe 4
//       result.totalInvalid shouldBe 1
//       result.killed shouldBe 2
//       result.survived shouldBe 1
//       result.compileErrors shouldBe 1
//     }
//   }

    it("should use static temp dir if it was requested") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[IOReporter]
      val rollbackHandler = mock[RollbackHandler]
      when(reporterMock.mutantTested).thenReturn(_.drain)
      val mutant = new MutantWithId(3, createMutant.mutatedCode)

      val testRunner = { (path: Path) =>
        // Static temp dir is used.
        path.toString should endWith("stryker4jvm-tmpDir")
        TestRunnerStub.withResults(mutant.toMutantResult(MutantStatus.Killed))(path)
      }

      val sut =
        new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)(staticTmpDirConfig, log)
      val file = FileUtil.getResource("mockFiles/simple.test")
      val mutants = Vector(mutant)
      val mutatedFile = MutatedFile(file, new MockAST(""), mutants)

      sut(Seq(mutatedFile)).asserting { _ =>
        // Cleaned up after run
        staticTmpDir.toNioPath.toFile shouldNot exist
      }
    }

    it("should not clean up tmp dir on errors") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[IOReporter]
      val rollbackHandler = mock[RollbackHandler]

      val testRunner = TestRunnerStub.withResults(initialTestRunResultIsSuccessful = false)()
      val mutant = new MutantWithId(3, createMutant.mutatedCode)

      val sut =
        new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)(staticTmpDirConfig, log)
      val file = FileUtil.getResource("mockFiles/simple.test")
      val mutants = Vector(mutant)
      val mutatedFile = MutatedFile(file, new MockAST(""), mutants)

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
      val reporterMock = mock[IOReporter]
      val rollbackHandler = mock[RollbackHandler]

      when(reporterMock.mutantTested).thenReturn(_.drain)
      val mutant = new MutantWithId(1, createMutant.mutatedCode)

      val testRunner = TestRunnerStub.withResults(mutant.toMutantResult(MutantStatus.Killed))

      val sut =
        new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)(noCleanTmpDirConfig, log)
      val file = FileUtil.getResource("mockFiles/simple.test")
      val mutants = Vector(mutant)
      val mutatedFile = MutatedFile(file, new MockAST(""), mutants)

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
