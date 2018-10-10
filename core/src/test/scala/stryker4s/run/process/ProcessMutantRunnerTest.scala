package stryker4s.run.process

import java.nio.file.Paths

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import stryker4s.Stryker4sSuite
import stryker4s.config.Config
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.ProcessMutantRunner
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.stubs.TestProcessRunner

import scala.concurrent.TimeoutException
import scala.meta._
import scala.util.{Failure, Success}

class ProcessMutantRunnerTest extends Stryker4sSuite with MockitoSugar with LogMatchers {

  private implicit val config: Config = Config(baseDir = FileUtil.getResource("scalaFiles"))
  private val fileCollectorMock: SourceCollector = mock[SourceCollector]

  describe("apply") {
    it("should return a Survived mutant on an exitcode 0 process") {
      val testProcessRunner = new TestProcessRunner(Success(0))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val mutant = Mutant(0, q"4", q"5")
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant))

      when(fileCollectorMock.filesToCopy(testProcessRunner)).thenReturn(List(file))

      val result = sut.apply(Seq(mutatedFile), fileCollectorMock)

      testProcessRunner.timesCalled.next() should equal(1)
      result.mutationScore shouldBe 00.00
      val loneResult = result.results.loneElement
      loneResult should equal(Survived(mutant, Paths.get("simpleFile.scala")))
    }

    it("should return a Killed mutant on an exitcode 1 process") {
      val testProcessRunner = new TestProcessRunner(Success(1))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val mutant = Mutant(0, q"4", q"5")
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant))

      when(fileCollectorMock.filesToCopy(testProcessRunner)).thenReturn(List(file))

      val result = sut.apply(Seq(mutatedFile), fileCollectorMock)

      testProcessRunner.timesCalled.next() should equal(1)
      result.mutationScore shouldBe 100.00
      val loneResult = result.results.loneElement
      loneResult should equal(Killed(1, mutant, Paths.get("simpleFile.scala")))
    }

    it("should return a TimedOut mutant on a TimedOut process") {
      val exception = new TimeoutException("Test")
      val testProcessRunner = new TestProcessRunner(Failure(exception))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val mutant = Mutant(0, q"4", q"5")
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant))

      when(fileCollectorMock.filesToCopy(testProcessRunner)).thenReturn(List(file))

      val result = sut.apply(Seq(mutatedFile), fileCollectorMock)

      testProcessRunner.timesCalled.next() should equal(1)
      result.mutationScore shouldBe 100.00
      val loneResult = result.results.loneElement
      loneResult should equal(TimedOut(exception, mutant, Paths.get("simpleFile.scala")))
    }

    it("should return a combination of results on multiple runs") {
      val testProcessRunner = new TestProcessRunner(Success(1), Success(1))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val mutant = Mutant(0, q"0", q"zero")
      val secondMutant = Mutant(1, q"1", q"one")
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = Seq(mutant, secondMutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants)

      when(fileCollectorMock.filesToCopy(testProcessRunner)).thenReturn(List(file))

      val result = sut.apply(Seq(mutatedFile), fileCollectorMock)

      testProcessRunner.timesCalled.next() should equal(2)

      result.mutationScore shouldBe 100.00
      result.results should contain only (
        Killed(1, mutant, Paths.get("simpleFile.scala")),
        Killed(1, secondMutant, Paths.get("simpleFile.scala"))
      )
    }

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed") {
      val testProcessRunner = new TestProcessRunner(Success(1), Success(1), Success(0))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
      val mutant = Mutant(0, q"0", q"zero")
      val secondMutant = Mutant(1, q"1", q"one")
      val thirdMutant = Mutant(2, q"5", q"5")
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = Seq(mutant, secondMutant, thirdMutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants)

      when(fileCollectorMock.filesToCopy(testProcessRunner)).thenReturn(List(file))

      val result = sut.apply(Seq(mutatedFile), fileCollectorMock)

      testProcessRunner.timesCalled.next() should equal(3)

      result.mutationScore shouldBe 66.67
      result.results should contain only (
        Killed(1, mutant, Paths.get("simpleFile.scala")),
        Killed(1, secondMutant, Paths.get("simpleFile.scala")),
        Survived(thirdMutant, Paths.get("simpleFile.scala"))
      )
    }

    describe("Log tests") {
      it("Should log that test run 1 is started and finished when mutant id is 0") {
        val testProcessRunner = new TestProcessRunner(Success(0))
        val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
        val mutant = Mutant(0, q"4", q"5")
        val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
        val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant))

        when(fileCollectorMock.filesToCopy(testProcessRunner)).thenReturn(List(file))

        sut.apply(Seq(mutatedFile), fileCollectorMock)

        "Starting test-run 1..." shouldBe loggedAsInfo
        "Finished mutation run 1/1 (100%)" shouldBe loggedAsInfo
      }

      it("Should log multiple test runs") {
        val testProcessRunner = new TestProcessRunner(Success(0), Success(0))
        val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner)
        val mutant0 = Mutant(0, q"4", q"5")
        val mutant1 = Mutant(1, q"4", q"5")
        val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
        val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant0, mutant1))

        when(fileCollectorMock.filesToCopy(testProcessRunner)).thenReturn(List(file))

        sut.apply(Seq(mutatedFile), fileCollectorMock)

        "Starting test-run 1..." shouldBe loggedAsInfo
        "Finished mutation run 1/2 (50%)" shouldBe loggedAsInfo
        "Starting test-run 2..." shouldBe loggedAsInfo
        "Finished mutation run 2/2 (100%)" shouldBe loggedAsInfo
      }
    }
  }
}
