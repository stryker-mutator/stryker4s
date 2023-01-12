package stryker4jvm.command.runner

import fs2.io.file.Path
import mutationtesting.MutantStatus
import stryker4jvm.config.Config
import stryker4jvm.model.*
import stryker4jvm.run.process.{Command, ProcessRunner}
import stryker4jvm.scalatest.LogMatchers
import stryker4jvm.testutil.stubs.TestProcessRunner
import stryker4jvm.testutil.{MockitoIOSuite, Stryker4jvmIOSuite, TestData}

import scala.concurrent.TimeoutException
import scala.util.{Failure, Success}

class ProcessTestRunnerTest extends Stryker4jvmIOSuite with MockitoIOSuite with LogMatchers with TestData {

  implicit val config: Config = Config.default
  def processTestRunner(processRunner: ProcessRunner) =
    new ProcessTestRunner(Command("foo", "test"), processRunner, Path("."))

  describe("runMutant") {

    it("should return a Survived mutant on an exitcode 0 process") {
      val testProcessRunner = TestProcessRunner(Success(0))
      processTestRunner(testProcessRunner).runMutant(createMutant, Seq.empty).asserting { result =>
        result.status shouldBe MutantStatus.Survived
        testProcessRunner.timesCalled.next() should equal(1)
      }
    }

    it("should return a Killed mutant on an exitcode 1 process") {
      val testProcessRunner = TestProcessRunner(Success(1))
      processTestRunner(testProcessRunner).runMutant(createMutant, Seq.empty).asserting { result =>
        result.status shouldBe MutantStatus.Killed
        testProcessRunner.timesCalled.next() should equal(1)
      }
    }

    it("should return a TimedOut mutant on a TimedOut process") {
      val exception = new TimeoutException("Test")
      val testProcessRunner = TestProcessRunner(Failure(exception))
      processTestRunner(testProcessRunner).runMutant(createMutant, Seq.empty).asserting { result =>
        result.status shouldBe MutantStatus.Timeout
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
