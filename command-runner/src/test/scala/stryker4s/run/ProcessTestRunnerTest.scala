package stryker4s.run

import java.nio.file.Paths

import scala.concurrent.TimeoutException
import scala.meta._
import scala.util.{Failure, Success}

import stryker4s.command.runner.ProcessTestRunner
import stryker4s.extension.mutationtype.EmptyString
import stryker4s.model._
import stryker4s.run.process.{Command, ProcessRunner}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.stubs.TestProcessRunner
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite}

class ProcessTestRunnerTest extends Stryker4sIOSuite with MockitoIOSuite with LogMatchers {

  def processTestRunner(processRunner: ProcessRunner) =
    new ProcessTestRunner(Command("foo", "test"), processRunner, Paths.get("."))

  describe("runMutant") {

    it("should return a Survived mutant on an exitcode 0 process") {
      val testProcessRunner = TestProcessRunner(Success(0))
      val mutant = Mutant(0, q"4", q"5", EmptyString)
      processTestRunner(testProcessRunner).runMutant(mutant).asserting { result =>
        result shouldBe a[Survived]
        testProcessRunner.timesCalled.next() should equal(1)
      }
    }

    it("should return a Killed mutant on an exitcode 1 process") {
      val testProcessRunner = TestProcessRunner(Success(1))
      val mutant = Mutant(0, q"4", q"5", EmptyString)
      processTestRunner(testProcessRunner).runMutant(mutant).asserting { result =>
        result shouldBe a[Killed]
        testProcessRunner.timesCalled.next() should equal(1)
      }
    }

    it("should return a TimedOut mutant on a TimedOut process") {
      val exception = new TimeoutException("Test")
      val testProcessRunner = TestProcessRunner(Failure(exception))
      val mutant = Mutant(0, q"4", q"5", EmptyString)
      processTestRunner(testProcessRunner).runMutant(mutant).asserting { result =>
        result shouldBe a[TimedOut]
        testProcessRunner.timesCalled.next() should equal(1)
      }
    }
  }

  describe("initialTestRun") {
    it("should throw an exception when the initial test run fails") {
      val testProcessRunner = TestProcessRunner.failInitialTestRun()
      processTestRunner(testProcessRunner).initialTestRun().asserting { result =>
        result shouldBe Left(false)
      }
    }
  }
}
