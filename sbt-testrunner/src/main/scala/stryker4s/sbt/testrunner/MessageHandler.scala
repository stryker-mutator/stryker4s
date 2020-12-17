package stryker4s.sbt.testrunner

import scala.util.control.NonFatal

import sbt.testing.Status
import stryker4s.api.testprocess._

trait MessageHandler {
  def handleMessage(req: Request): Response
}

final class TestRunnerMessageHandler() extends MessageHandler {
  private var testRunner: SbtTestInterfaceRunner = null

  def handleMessage(req: Request): Response =
    req match {
      case StartTestRun(mutation) =>
        try {
          val status = testRunner.runMutation(mutation)
          toTestResult(status)
        } catch {
          case NonFatal(e) => ErrorDuringTestRun(e.toString())
        }

      case StartInitialTestRun() =>
        val (status, report) = stryker4s.coverage.collectCoverage {
          testRunner.initialTestRun()
        }
        toInitialTestResult(status, report)
      case SetupTestContext(testContext) =>
        testRunner = new SbtTestInterfaceRunner(testContext)
        println("Set up testContext")
        SetupTestContextSuccessful()
    }

  def toTestResult(status: Status): TestResultResponse =
    status match {
      case Status.Success => TestsSuccessful()
      case _              => TestsUnsuccessful()
    }

  def toInitialTestResult(status: Status, coverage: CoverageReport): TestResultResponse =
    status match {
      case Status.Success => CoverageTestRunResult(true, coverage)
      case _              => CoverageTestRunResult(false, coverage)
    }

}
