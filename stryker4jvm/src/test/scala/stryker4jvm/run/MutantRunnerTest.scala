package stryker4jvm.run

import cats.data.NonEmptyList
import mutationtesting.MutantStatus
import stryker4jvm.config.Config
import stryker4jvm.mutator.scala.scalatest.{FileUtil, LogMatchers}
import stryker4jvm.mutator.scala.testutil.{MockitoIOSuite, TestData}
import stryker4jvm.reporting.{FinishedRunEvent, Reporter}

class MutantRunnerTest extends Stryker4jvmIOSuite with MockitoIOSuite with LogMatchers with TestData {

  describe("apply") {
    implicit val config = Config.default.copy(baseDir = FileUtil.getResource("scalaFiles"))

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[Reporter]
      val rollbackHandler = mock[RollbackHandler]
      whenF(reporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
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
      val mutants = Seq(mutant, secondMutant, thirdMutant)
      // val mutatedFile = MutatedFile(file, q"def foo = 4", mutants, Seq.empty, 0)
      fail()
      // sut(_ => IO.pure(List(mutatedFile))).asserting { result =>
      //   val captor = ArgCaptor[FinishedRunEvent]
      //   verify(reporterMock, times(1)).onRunFinished(captor.capture)
      //   val runReport = captor.value.report.files.loneElement

      //   "Setting up mutated environment..." shouldBe loggedAsInfo
      //   "Starting initial test run..." shouldBe loggedAsInfo
      //   "Initial test run succeeded! Testing mutants..." shouldBe loggedAsInfo
      //   runReport._1 shouldBe "simpleFile.scala"
      //   runReport._2.mutants.map(_.id) shouldBe List("1", "2", "3")
      //   result.mutationScore shouldBe ((2d / 3d) * 100)
      //   result.totalMutants shouldBe 3
      //   result.killed shouldBe 2
      //   result.survived shouldBe 1
      // }
    }

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed and 1 doesn't compile.") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[Reporter]
      val rollbackHandler = mock[RollbackHandler]
      whenF(reporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      when(reporterMock.mutantTested).thenReturn(_.drain)

      val mutant = createMutant.copy(id = MutantId(3))
      val secondMutant = createMutant.copy(id = MutantId(1))
      val thirdMutant = createMutant.copy(id = MutantId(2))
      val nonCompilingMutant = createMutant.copy(id = MutantId(4))

      val errs = NonEmptyList.one(CompilerErrMsg("blah", "xyz", 123))
      val testRunner =
        TestRunnerStub.withInitialCompilerError(
          errs,
          mutant.toMutantResult(MutantStatus.Killed),
          secondMutant.toMutantResult(MutantStatus.Killed),
          thirdMutant.toMutantResult(MutantStatus.Survived),
          nonCompilingMutant.toMutantResult(MutantStatus.CompileError)
        )

      val sut = new MutantRunner(testRunner, fileCollectorMock, rollbackHandler, reporterMock)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = Seq(mutant, secondMutant, thirdMutant)
      // val mutatedFile = MutatedFile(file, q"def foo = 4", mutants, Seq(nonCompilingMutant), 0)
      fail()
      // sut(_ => IO(List(mutatedFile))).asserting { result =>
      //   val captor = ArgCaptor[FinishedRunEvent]
      //   verify(reporterMock, times(1)).onRunFinished(captor.capture)
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
    }
  }
}
