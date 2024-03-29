package stryker4s.sbt.testrunner

import sbt.testing.Status
import stryker4s.coverage.{collectCoverage, timed}
import stryker4s.testrunner.api.*

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

trait MessageHandler {
  def handleMessage(req: Request): Response
}

final class TestRunnerMessageHandler() extends MessageHandler {
  private var testRunner: SbtTestInterfaceRunner = null

  def handleMessage(req: Request): Response =
    req match {
      case StartTestRun(mutation, testNames) =>
        try {
          val result = testRunner.runMutation(mutation, testNames)
          toTestResult(result)
        } catch {
          case NonFatal(e) => ErrorDuringTestRun.of(e.toString())
        }

      case StartInitialTestRun() =>
        val ((duration, result), report) = collectCoverage {
          timed {
            testRunner.initialTestRun()
          }
        }
        toInitialTestResult(result.status, report, duration)
      case testContext: TestProcessContext =>
        testRunner = new SbtTestInterfaceRunner(testContext)
        println("Set up testContext")
        SetupTestContextSuccessful()
      case Request.Empty => throw new MatchError(req)
    }

  def toTestResult(result: TestRunResult): Response =
    if (result.status == Status.Success) TestsSuccessful.of(result.testsCompleted)
    else TestsUnsuccessful.of(result.testsCompleted, failedTests = result.failedTests)

  def toInitialTestResult(status: Status, coverage: CoverageTestNameMap, duration: FiniteDuration): Response =
    CoverageTestRunResult.of(status == Status.Success, Some(coverage), durationNanos = duration.toNanos)

}
