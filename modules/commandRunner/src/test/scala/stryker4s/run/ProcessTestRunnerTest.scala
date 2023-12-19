package stryker4s.run

import cats.syntax.option.*
import fs2.io.file.Path
import mutationtesting.{Location, MutantStatus, Position}
import stryker4s.command.runner.ProcessTestRunner
import stryker4s.config.Config
import stryker4s.model.*
import stryker4s.mutation.GreaterThan
import stryker4s.run.process.{Command, ProcessRunner}
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}
import stryker4s.testutil.stubs.TestProcessRunner

import scala.concurrent.TimeoutException
import scala.meta.quasiquotes.*
import scala.util.{Failure, Success}

class ProcessTestRunnerTest extends Stryker4sIOSuite with LogMatchers {

  implicit val config: Config = Config.default
  def processTestRunner(processRunner: ProcessRunner) =
    new ProcessTestRunner(Command("foo", "test"), processRunner, Path("."))

  describe("runMutant") {

    test("should return a Survived mutant on an exitcode 0 process") {
      val testProcessRunner = TestProcessRunner(Success(0))
      processTestRunner(testProcessRunner).runMutant(createMutant, Seq.empty).asserting { result =>
        assertEquals(result.status, MutantStatus.Survived)
        assertEquals(testProcessRunner.timesCalled.next(), 1)
      }
    }

    test("should return a Killed mutant on an exitcode 1 process") {
      val testProcessRunner = TestProcessRunner(Success(1))
      processTestRunner(testProcessRunner).runMutant(createMutant, Seq.empty).asserting { result =>
        assertEquals(result.status, MutantStatus.Killed)
        assertEquals(testProcessRunner.timesCalled.next(), 1)
      }
    }

    test("should return a TimedOut mutant on a TimedOut process") {
      val exception = new TimeoutException("Test")
      val testProcessRunner = TestProcessRunner(Failure(exception))
      processTestRunner(testProcessRunner).runMutant(createMutant, Seq.empty).asserting { result =>
        assertEquals(result.status, MutantStatus.Timeout)
        assertEquals(testProcessRunner.timesCalled.next(), 1)
      }
    }
  }

  describe("initialTestRun") {
    test("should have isSuccessful false when the initial test run fails") {
      val testProcessRunner = TestProcessRunner.failInitialTestRun()
      processTestRunner(testProcessRunner)
        .initialTestRun()
        .assertEquals(NoCoverageInitialTestRun(false))
    }
  }

  def createMutant =
    MutantWithId(
      MutantId(0),
      MutatedCode(q"<", MutantMetadata(">", "<", GreaterThan.mutationName, createLocation, none))
    )

  def createLocation = Location(Position(0, 0), Position(0, 0))

}
