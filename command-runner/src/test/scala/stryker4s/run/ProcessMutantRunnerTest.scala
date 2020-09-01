package stryker4s.run

import stryker4s.command.runner.ProcessMutantRunner
import stryker4s.config.Config
import stryker4s.extension.exception.InitialTestRunFailedException
import stryker4s.extension.mutationtype.EmptyString
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.AggregateReporter
import stryker4s.run.process.Command
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.stubs.TestProcessRunner
import stryker4s.testutil.{MockitoSuite, Stryker4sSuite}

import scala.concurrent.TimeoutException
import scala.meta._
import scala.util.{Failure, Success}
import stryker4s.report.FinishedRunReport
import cats.effect.IO

class ProcessMutantRunnerTest extends Stryker4sSuite with MockitoSuite with LogMatchers {
  implicit private val config: Config = Config(baseDir = FileUtil.getResource("scalaFiles"))
  private val fileCollectorMock: SourceCollector = mock[SourceCollector]
  private val reporterMock = mock[AggregateReporter]
  when(reporterMock.reportRunFinished(any[FinishedRunReport])).thenReturn(IO.unit)
  when(reporterMock.reportMutationComplete(any[MutantRunResult], anyInt)).thenReturn(IO.unit)
  when(reporterMock.reportMutationStart(any[Mutant])).thenReturn(IO.unit)

  describe("apply") {
    it("should return a Survived mutant on an exitcode 0 process") {
      val testProcessRunner = TestProcessRunner(Success(0))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock, reporterMock)
      val mutant = Mutant(0, q"4", q"5", EmptyString)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant), 0)

      when(fileCollectorMock.filesToCopy).thenReturn(List(file))

      val result = sut(List(mutatedFile)).unsafeRunSync()

      testProcessRunner.timesCalled.next() should equal(1)
      result.mutationScore shouldBe 00.00
      result.totalMutants shouldBe 1
      result.survived shouldBe 1
    }

    it("should return a Killed mutant on an exitcode 1 process") {
      val testProcessRunner = TestProcessRunner(Success(1))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock, reporterMock)
      val mutant = Mutant(0, q"4", q"5", EmptyString)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant), 0)

      when(fileCollectorMock.filesToCopy).thenReturn(List(file))

      val result = sut(List(mutatedFile)).unsafeRunSync()

      testProcessRunner.timesCalled.next() should equal(1)
      result.mutationScore shouldBe 100.00
      result.totalMutants shouldBe 1
      result.killed shouldBe 1
    }

    it("should return a TimedOut mutant on a TimedOut process") {
      val exception = new TimeoutException("Test")
      val testProcessRunner = TestProcessRunner(Failure(exception))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock, reporterMock)
      val mutant = Mutant(0, q"4", q"5", EmptyString)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant), 0)

      when(fileCollectorMock.filesToCopy).thenReturn(List(file))

      val result = sut(List(mutatedFile)).unsafeRunSync()

      testProcessRunner.timesCalled.next() should equal(1)
      result.mutationScore shouldBe 100.00
      result.totalMutants shouldBe 1
      result.timeout shouldBe 1
    }

    it("should return a combination of results on multiple runs") {
      val testProcessRunner = TestProcessRunner(Success(1), Success(1))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock, reporterMock)
      val mutant = Mutant(0, q"0", q"zero", EmptyString)
      val secondMutant = Mutant(1, q"1", q"one", EmptyString)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = Seq(mutant, secondMutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants, 0)

      when(fileCollectorMock.filesToCopy).thenReturn(List(file))

      val result = sut(List(mutatedFile)).unsafeRunSync()

      testProcessRunner.timesCalled.next() should equal(2)

      result.mutationScore shouldBe 100.00
      result.totalMutants shouldBe 2
      result.killed shouldBe 2
    }

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed") {
      val testProcessRunner = TestProcessRunner(Success(1), Success(1), Success(0))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock, reporterMock)
      val mutant = Mutant(0, q"0", q"zero", EmptyString)
      val secondMutant = Mutant(1, q"1", q"one", EmptyString)
      val thirdMutant = Mutant(2, q"5", q"5", EmptyString)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = Seq(mutant, secondMutant, thirdMutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants, 0)

      when(fileCollectorMock.filesToCopy).thenReturn(List(file))

      val result = sut(List(mutatedFile)).unsafeRunSync()

      testProcessRunner.timesCalled.next() should equal(3)

      result.mutationScore shouldBe ((2d / 3d) * 100)
      result.totalMutants shouldBe 3
      result.killed shouldBe 2
      result.survived shouldBe 1
    }

    it("should throw an exception when the initial test run fails") {
      val testProcessRunner = TestProcessRunner.failInitialTestRun()
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock, reporterMock)

      when(fileCollectorMock.filesToCopy).thenReturn(List.empty)

      an[InitialTestRunFailedException] shouldBe thrownBy(sut(List.empty).unsafeRunSync())
    }
    describe("Log tests") {
      it("should properly log the initial test run") {
        val testProcessRunner = TestProcessRunner()
        val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock, reporterMock)

        when(fileCollectorMock.filesToCopy).thenReturn(List.empty)

        sut(List.empty).unsafeRunSync()

        "Starting initial test run..." shouldBe loggedAsInfo
        "Initial test run succeeded! Testing mutants..." shouldBe loggedAsInfo
      }
    }
  }
}
