package stryker4jvm.run

import cats.data.NonEmptyVector
import cats.effect.IO
import fs2.io.file.{Files, Path}
import mutationtesting.MutantStatus
import stryker4jvm.config.Config
import stryker4jvm.reporting.IOReporter
import stryker4jvm.scalatest.{FileUtil, LogMatchers}
import stryker4jvm.testutil.{MockitoIOSuite, Stryker4jvmIOSuite, TestData}
import stryker4jvm.testutil.stubs.TestFileResolver

import scala.meta.*

class MutantRunnerTest extends Stryker4jvmIOSuite with MockitoIOSuite with LogMatchers with TestData {
//    TODO: test MutantRunner without scala-mutator

//  describe("apply") {
//    val baseDir = FileUtil.getResource("scalaFiles")
//    val staticTmpDir = baseDir.resolve("target/stryker4s-tmpDir")
//
//    implicit val config = Config.default.copy(baseDir = baseDir)
//
//    val staticTmpDirConfig = config.copy(staticTmpDir = true)
//    val noCleanTmpDirConfig = staticTmpDirConfig.copy(cleanTmpDir = false)
//
//    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed") {
//      val fileCollectorMock = new TestFileResolver(Seq.empty)
//      val reporterMock = mock[IOReporter[Config]]
//      val rollbackHandler = mock[RollbackHandler]
//      when(reporterMock.mutantTested).thenReturn(_.drain)
//      val mutant = createMutant.copy(id = MutantId(3))
//      val secondMutant = createMutant.copy(id = MutantId(1))
//      val thirdMutant = createMutant.copy(id = MutantId(2))
//
//      val testRunner = { (path: Path) =>
//        // Static temp dir is not used with default settings.
//        path.toString should not endWith "stryker4s-tmpDir"
//        TestRunnerStub.withResults(
//          mutant.toMutantResult(MutantStatus.Killed),
//          secondMutant.toMutantResult(MutantStatus.Killed),
//          thirdMutant.toMutantResult(MutantStatus.Survived)
//        )(path)
//      }
//
//      val sut = new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)
//      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
//      val mutants = NonEmptyVector.of(mutant, secondMutant, thirdMutant)
//      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants)
//      sut(Seq(mutatedFile)).asserting { case RunResult(results, _) =>
//        "Setting up mutated environment..." shouldBe loggedAsInfo
//        "Starting initial test run..." shouldBe loggedAsInfo
//        "Initial test run succeeded! Testing mutants..." shouldBe loggedAsInfo
//        val (path, resultForFile) = results.loneElement
//        path shouldBe file
//        resultForFile.map(_.status) shouldBe List(MutantStatus.Killed, MutantStatus.Survived, MutantStatus.Killed)
//      }
//    }

  // it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed and 1 doesn't compile.") {
  //   val fileCollectorMock = new TestFileResolver(Seq.empty)
  //   val reporterMock = mock[Reporter]
  //   val rollbackHandler = mock[RollbackHandler]
  //   when(reporterMock.mutantTested).thenReturn(_.drain)

  //   val mutant = createMutant.copy(id = MutantId(3))
  //   val secondMutant = createMutant.copy(id = MutantId(1))
  //   val thirdMutant = createMutant.copy(id = MutantId(2))
  //   val nonCompilingMutant = createMutant.copy(id = MutantId(4))

  //   val errs = NonEmptyList.one(CompilerErrMsg("blah", "xyz", 123))
  //   val testRunner =
  //     TestRunnerStub.withInitialCompilerError(
  //       errs,
  //       mutant.toMutantResult(MutantStatus.Killed),
  //       secondMutant.toMutantResult(MutantStatus.Killed),
  //       thirdMutant.toMutantResult(MutantStatus.Survived),
  //       nonCompilingMutant.toMutantResult(MutantStatus.CompileError)
  //     )

  //   val sut = new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)
  //   val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
  //   val mutants = Seq(mutant, secondMutant, thirdMutant)
  //   // val mutatedFile = MutatedFile(file, q"def foo = 4", mutants, Seq(nonCompilingMutant), 0)
  //   fail()
  // sut(_ => IO(List(mutatedFile))).asserting { result =>
  //   val captor = ArgCaptor[FinishedRunEvent]
  //   val runReport = captor.value.report.files.loneElement

  //   "Setting up mutated environment..." shouldBe loggedAsInfo
  //   "Starting initial test run..." shouldBe loggedAsInfo
  //   "Initial test run succeeded! Testing mutants..." shouldBe loggedAsInfo
  //   "Attempting to remove mutants that gave a compile error..." shouldBe loggedAsInfo

  //   runReport._1 shouldBe "simpleFile.scala"
  //   runReport._2.mutants.map(_.id) shouldBe List("1", "2", "3", "4")
  //   result.mutationScore shouldBe ((2d / 3d) * 100)
  //   result.totalMutants shouldBe 4
  //   result.totalInvalid shouldBe 1
  //   result.killed shouldBe 2
  //   result.survived shouldBe 1
  //   result.compileErrors shouldBe 1
  // }

//    it("should use static temp dir if it was requested") {
//      val fileCollectorMock = new TestFileResolver(Seq.empty)
//      val reporterMock = mock[IOReporter[Config]]
//      val rollbackHandler = mock[RollbackHandler]
//      when(reporterMock.mutantTested).thenReturn(_.drain)
//      val mutant = createMutant.copy(id = MutantId(3))
//
//      val testRunner = { (path: Path) =>
//        // Static temp dir is used.
//        path.toString should endWith("stryker4s-tmpDir")
//        TestRunnerStub.withResults(mutant.toMutantResult(MutantStatus.Killed))(path)
//      }
//
//      val sut =
//        new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)(staticTmpDirConfig, testLogger)
//      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
//      val mutants = NonEmptyVector.one(mutant)
//      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants)
//
//      sut(Seq(mutatedFile)).asserting { _ =>
//        // Cleaned up after run
//        staticTmpDir.toNioPath.toFile shouldNot exist
//      }
//    }

//    it("should not clean up tmp dir on errors") {
//      val fileCollectorMock = new TestFileResolver(Seq.empty)
//      val reporterMock = mock[Reporter]
//      val rollbackHandler = mock[RollbackHandler]
//
//      val testRunner = TestRunnerStub.withResults(initialTestRunResultIsSuccessful = false)()
//      val mutant = createMutant.copy(id = MutantId(3))
//
//      val sut =
//        new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)(staticTmpDirConfig, testLogger)
//      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
//      val mutants = NonEmptyVector.one(mutant)
//      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants)
//
//      sut(Seq(mutatedFile)).attempt
//        .asserting { result =>
//          staticTmpDir.toNioPath.toFile should exist
//
//          result shouldBe a[Left[Throwable, ?]]
//          result.asInstanceOf[Left[Throwable, ?]].value.getMessage should startWith("Initial test run failed")
//        }
//        .flatMap { result =>
//          // Simulate the user manually cleaned up the tmp dir (before we run the next test case).
//          Files[IO].deleteRecursively(staticTmpDir).as(result)
//        }
//    }

//    it("should not clean up tmp dir if clean-tmp-dir is disabled") {
//      val fileCollectorMock = new TestFileResolver(Seq.empty)
//      val reporterMock = mock[Reporter]
//      val rollbackHandler = mock[RollbackHandler]
//
//      when(reporterMock.mutantTested).thenReturn(_.drain)
//      val mutant = createMutant.copy(id = MutantId(1))
//
//      val testRunner = TestRunnerStub.withResults(mutant.toMutantResult(MutantStatus.Killed))
//
//      val sut =
//        new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)(noCleanTmpDirConfig, testLogger)
//      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
//      val mutants = NonEmptyVector.one(mutant)
//      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants)
//
//      sut(Seq(mutatedFile))
//        .asserting { _ =>
//          staticTmpDir.toNioPath.toFile should exist
//        }
//        .flatMap { result =>
//          // Simulate the user manually cleaned up the tmp dir (before we run the next test case).
//          Files[IO].deleteRecursively(staticTmpDir).as(result)
//        }
//    }
//  }
}
