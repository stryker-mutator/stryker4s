package stryker4s.run.process

import java.nio.file.Paths

import org.mockito.integrations.scalatest.MockitoFixture
import stryker4s.config.Config
import stryker4s.extension.exception.InitialTestRunFailedException
import stryker4s.extension.mutationtype.EmptyString
import stryker4s.model._
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.scalatest.{FileUtil, LogMatchers}
import stryker4s.testutil.stubs.TestProcessRunner
import stryker4s.testutil.Stryker4sSuite

import scala.concurrent.TimeoutException
import scala.meta._
import scala.util.{Failure, Success}

class ProcessMutantRunnerTest extends Stryker4sSuite with MockitoFixture with LogMatchers {

  private implicit val config: Config = Config(baseDir = FileUtil.getResource("scalaFiles"))
  private val fileCollectorMock: SourceCollector = mock[SourceCollector]

  describe("apply") {
    it("should return a Survived mutant on an exitcode 0 process") {
      val testProcessRunner = TestProcessRunner(Success(0))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock)
      val mutant = Mutant(0, q"4", q"5", EmptyString)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant), 0)

      when(fileCollectorMock.filesToCopy).thenReturn(List(file))

      val result = sut(Seq(mutatedFile))

      testProcessRunner.timesCalled.next() should equal(1)
      result.mutationScore shouldBe 00.00
      val loneResult = result.results.loneElement
      loneResult should equal(Survived(mutant, Paths.get("simpleFile.scala")))
    }

    it("should return a Killed mutant on an exitcode 1 process") {
      val testProcessRunner = TestProcessRunner(Success(1))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock)
      val mutant = Mutant(0, q"4", q"5", EmptyString)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant), 0)

      when(fileCollectorMock.filesToCopy).thenReturn(List(file))

      val result = sut(Seq(mutatedFile))

      testProcessRunner.timesCalled.next() should equal(1)
      result.mutationScore shouldBe 100.00
      val loneResult = result.results.loneElement
      loneResult should equal(Killed(mutant, Paths.get("simpleFile.scala")))
    }

    it("should return a TimedOut mutant on a TimedOut process") {
      val exception = new TimeoutException("Test")
      val testProcessRunner = TestProcessRunner(Failure(exception))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock)
      val mutant = Mutant(0, q"4", q"5", EmptyString)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant), 0)

      when(fileCollectorMock.filesToCopy).thenReturn(List(file))

      val result = sut(Seq(mutatedFile))

      testProcessRunner.timesCalled.next() should equal(1)
      result.mutationScore shouldBe 100.00
      val loneResult = result.results.loneElement
      loneResult should equal(TimedOut(mutant, Paths.get("simpleFile.scala")))
    }

    it("should return a combination of results on multiple runs") {
      val testProcessRunner = TestProcessRunner(Success(1), Success(1))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock)
      val mutant = Mutant(0, q"0", q"zero", EmptyString)
      val secondMutant = Mutant(1, q"1", q"one", EmptyString)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = Seq(mutant, secondMutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants, 0)

      when(fileCollectorMock.filesToCopy).thenReturn(List(file))

      val result = sut(Seq(mutatedFile))

      testProcessRunner.timesCalled.next() should equal(2)

      result.mutationScore shouldBe 100.00
      result.results should contain only (
        Killed(mutant, Paths.get("simpleFile.scala")),
        Killed(secondMutant, Paths.get("simpleFile.scala"))
      )
    }

    it("should return a mutationScore of 66.67 when 2 of 3 mutants are killed") {
      val testProcessRunner = TestProcessRunner(Success(1), Success(1), Success(0))
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock)
      val mutant = Mutant(0, q"0", q"zero", EmptyString)
      val secondMutant = Mutant(1, q"1", q"one", EmptyString)
      val thirdMutant = Mutant(2, q"5", q"5", EmptyString)
      val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
      val mutants = Seq(mutant, secondMutant, thirdMutant)
      val mutatedFile = MutatedFile(file, q"def foo = 4", mutants, 0)

      when(fileCollectorMock.filesToCopy).thenReturn(List(file))

      val result = sut(Seq(mutatedFile))

      testProcessRunner.timesCalled.next() should equal(3)

      result.mutationScore shouldBe 66.67
      result.results should contain only (
        Killed(mutant, Paths.get("simpleFile.scala")),
        Killed(secondMutant, Paths.get("simpleFile.scala")),
        Survived(thirdMutant, Paths.get("simpleFile.scala"))
      )
    }

    it("should throw an exception when the initial test run fails") {
      val testProcessRunner = TestProcessRunner.failInitialTestRun()
      val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock)

      when(fileCollectorMock.filesToCopy).thenReturn(List.empty)

      a[InitialTestRunFailedException] shouldBe thrownBy(sut(Seq.empty))
    }

    describe("Log tests") {
      it("Should log that test run 1 is started and finished when mutant id is 0") {
        val testProcessRunner = TestProcessRunner(Success(0))
        val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock)
        val mutant = Mutant(0, q"4", q"5", EmptyString)
        val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
        val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant), 0)

        when(fileCollectorMock.filesToCopy).thenReturn(List(file))

        sut(Seq(mutatedFile))

        "Starting test-run 1..." shouldBe loggedAsInfo
        "Finished mutation run 1/1 (100%)" shouldBe loggedAsInfo
      }

      it("Should log multiple test runs") {
        val testProcessRunner = TestProcessRunner(Success(0), Success(0))
        val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock)
        val mutant0 = Mutant(0, q"4", q"5", EmptyString)
        val mutant1 = Mutant(1, q"4", q"5", EmptyString)
        val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
        val mutatedFile = MutatedFile(file, q"def foo = 4", Seq(mutant0, mutant1), 0)

        when(fileCollectorMock.filesToCopy).thenReturn(List(file))

        sut(Seq(mutatedFile))

        "Starting test-run 1..." shouldBe loggedAsInfo
        "Finished mutation run 1/2 (50%)" shouldBe loggedAsInfo
        "Starting test-run 2..." shouldBe loggedAsInfo
        "Finished mutation run 2/2 (100%)" shouldBe loggedAsInfo
      }

      it("should properly log the initial test run") {
        val testProcessRunner = TestProcessRunner()
        val sut = new ProcessMutantRunner(Command("foo", "test"), testProcessRunner, fileCollectorMock)

        when(fileCollectorMock.filesToCopy).thenReturn(List.empty)

        sut(Seq.empty)

        "Starting initial test run..." shouldBe loggedAsInfo
        "Initial test run succeeded! Testing mutants..." shouldBe loggedAsInfo
      }
    }
  }
}
