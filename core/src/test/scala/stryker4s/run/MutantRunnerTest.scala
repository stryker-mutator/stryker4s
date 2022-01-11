package stryker4s.run

import cats.effect.IO
import org.mockito.captor.ArgCaptor
import stryker4s.config.Config
import stryker4s.extension.mutationtype.EmptyString
import stryker4s.model.*
import stryker4s.report.{FinishedRunEvent, Reporter}
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.stubs.{TestFileResolver, TestRunnerStub}
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite}

import scala.meta.*

class MutantRunnerTest extends Stryker4sIOSuite with MockitoIOSuite with LogMatchers {

  describe("apply") {
    implicit val config = Config.default.copy(baseDir = FileUtil.getResource("scalaFiles"))

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[Reporter]
      whenF(reporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      when(reporterMock.mutantTested).thenReturn(_.drain)
      val mutant = Mutant(MutantId(3), q"0", q"zero", EmptyString)
      val secondMutant = Mutant(MutantId(1), q"1", q"one", EmptyString)
      val thirdMutant = Mutant(MutantId(2), q"5", q"5", EmptyString)

      val testRunner = TestRunnerStub.withResults(Killed(mutant), Killed(secondMutant), Survived(thirdMutant))
      val sut = new MutantRunner(testRunner, fileCollectorMock, reporterMock)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = Seq(mutant, secondMutant, thirdMutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants, Seq.empty, 0)

      sut(_ => IO.pure(List(mutatedFile))).asserting { result =>
        val captor = ArgCaptor[FinishedRunEvent]
        verify(reporterMock, times(1)).onRunFinished(captor.capture)
        val runReport = captor.value.report.files.loneElement

        "Setting up mutated environment..." shouldBe loggedAsInfo
        "Starting initial test run..." shouldBe loggedAsInfo
        "Initial test run succeeded! Testing mutants..." shouldBe loggedAsInfo
        runReport._1 shouldBe "simpleFile.scala"
        runReport._2.mutants.map(_.id) shouldBe List("1", "2", "3")
        result.mutationScore shouldBe ((2d / 3d) * 100)
        result.totalMutants shouldBe 3
        result.killed shouldBe 2
        result.survived shouldBe 1
      }
    }

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed and 1 doesn't compile.") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[Reporter]
      whenF(reporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      when(reporterMock.mutantTested).thenReturn(_.drain)

      val mutant = Mutant(MutantId(3), q"0", q"zero", EmptyString)
      val secondMutant = Mutant(MutantId(1), q"1", q"one", EmptyString)
      val thirdMutant = Mutant(MutantId(2), q"5", q"5", EmptyString)
      val nonCompilingMutant = Mutant(MutantId(4), q"7", q"2", EmptyString)

      val errs = List(CompilerErrMsg("blah", "xyz", 123))
      val testRunner =
        TestRunnerStub.withInitialCompilerError(
          errs,
          Killed(mutant),
          Killed(secondMutant),
          Survived(thirdMutant),
          CompileError(nonCompilingMutant)
        )

      val sut = new MutantRunner(testRunner, fileCollectorMock, reporterMock)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = Seq(mutant, secondMutant, thirdMutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants, Seq(nonCompilingMutant), 0)

      sut(_ => IO(List(mutatedFile))).asserting { result =>
        val captor = ArgCaptor[FinishedRunEvent]
        verify(reporterMock, times(1)).onRunFinished(captor.capture)
        val runReport = captor.value.report.files.loneElement

        "Setting up mutated environment..." shouldBe loggedAsInfo
        "Starting initial test run..." shouldBe loggedAsInfo
        "Initial test run succeeded! Testing mutants..." shouldBe loggedAsInfo
        "Attempting to remove mutants that gave a compile error..." shouldBe loggedAsInfo

        runReport._1 shouldBe "simpleFile.scala"
        runReport._2.mutants.map(_.id) shouldBe List("1", "2", "3", "4")
        result.mutationScore shouldBe ((2d / 3d) * 100)
        result.totalMutants shouldBe 4
        result.totalInvalid shouldBe 1
        result.killed shouldBe 2
        result.survived shouldBe 1
        result.compileErrors shouldBe 1
      }
    }
  }
}
