package stryker4s.run

import org.mockito.captor.ArgCaptor
import stryker4s.config.Config
import stryker4s.extension.mutationtype.EmptyString
import stryker4s.model.{Killed, Mutant, MutantId, MutatedFile, Survived}
import stryker4s.report.{FinishedRunEvent, Reporter}
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.stubs.{TestFileResolver, TestRunnerStub}
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite}

import scala.meta._

class MutantRunnerTest extends Stryker4sIOSuite with MockitoIOSuite with LogMatchers {

  describe("apply") {
    implicit val config = Config.default.copy(baseDir = FileUtil.getResource("scalaFiles"))

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed") {
      val fileCollectorMock = new TestFileResolver(Seq.empty)
      val reporterMock = mock[Reporter]
      whenF(reporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())
      when(reporterMock.mutantTested).thenReturn(_.drain)
      val mutant = Mutant(MutantId(3, "scalaFiles/simpleFile.scala", 3), q"0", q"zero", EmptyString)
      val secondMutant = Mutant(MutantId(1, "scalaFiles/simpleFile.scala", 1), q"1", q"one", EmptyString)
      val thirdMutant = Mutant(MutantId(2, "scalaFiles/simpleFile.scala", 2), q"5", q"5", EmptyString)

      val testRunner = TestRunnerStub.withResults(Killed(mutant), Killed(secondMutant), Survived(thirdMutant))
      val sut = new MutantRunner(testRunner, fileCollectorMock, reporterMock)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = Seq(mutant, secondMutant, thirdMutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants, Seq.empty, 0)

      sut(List(mutatedFile), Seq.empty).asserting { result =>
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
  }
}
