package stryker4s.run

import fs2.io.file.Path
import stryker4s.command.runner.ProcessTestRunner
import stryker4s.config.Config
import stryker4s.extension.mutationtype.EmptyString
import stryker4s.model._
import stryker4s.run.process.{Command, ProcessRunner}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.stubs.TestProcessRunner
import stryker4s.testutil.{MockitoIOSuite, Stryker4sIOSuite}

import scala.concurrent.TimeoutException
import scala.meta._
import scala.util.{Failure, Success}

class ProcessTestRunnerTest extends Stryker4sIOSuite with MockitoIOSuite with LogMatchers {

  implicit val config: Config = Config.default
  def processTestRunner(processRunner: ProcessRunner) =
    new ProcessTestRunner(Command("foo", "test"), processRunner, Path("."))

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
    it("should have isSuccessful false when the initial test run fails") {
      val testProcessRunner = TestProcessRunner.failInitialTestRun()
      processTestRunner(testProcessRunner).initialTestRun().asserting { result =>
        result shouldBe NoCoverageInitialTestRun(false)
      }
    }
  }
}
