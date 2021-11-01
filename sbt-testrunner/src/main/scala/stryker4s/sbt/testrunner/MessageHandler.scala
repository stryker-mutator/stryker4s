package stryker4s.sbt.testrunner

import sbt.testing.Status
import stryker4s.api.testprocess._

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
          case NonFatal(e) => ErrorDuringTestRun(e.toString())
        }

      case StartInitialTestRun() =>
        val (result, report) = stryker4s.coverage.collectCoverage {
          testRunner.initialTestRun()
        }
        toInitialTestResult(result.status, report)
      case testContext: TestProcessContext =>
        testRunner = new SbtTestInterfaceRunner(testContext)
        println("Set up testContext")
        SetupTestContextSuccessful()
      case Request.Empty => throw new MatchError(req)
    }

  def toTestResult(result: TestRunResult): Response =
    if (result.status == Status.Success) TestsSuccessful(result.testsCompleted)
    else TestsUnsuccessful(result.testsCompleted)

  def toInitialTestResult(status: Status, coverage: CoverageTestNameMap): Response =
    CoverageTestRunResult(status == Status.Success, Some(coverage))

}
