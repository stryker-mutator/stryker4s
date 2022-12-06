package stryker4s.run

import cats.data.NonEmptyVector
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
    implicit val config = Config.default.copy(baseDir = FileUtil.getResource("scalaFiles"))

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[Reporter]
      val rollbackHandler = mock[RollbackHandler]
      when(reporterMock.mutantTested).thenReturn(_.drain)
      val mutant = createMutant.copy(id = MutantId(3))
      val secondMutant = createMutant.copy(id = MutantId(1))
      val thirdMutant = createMutant.copy(id = MutantId(2))

      val testRunner = TestRunnerStub.withResults(
        mutant.toMutantResult(MutantStatus.Killed),
        secondMutant.toMutantResult(MutantStatus.Killed),
        thirdMutant.toMutantResult(MutantStatus.Survived)
      )
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
    // }
  }
}
