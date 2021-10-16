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
      case testContext: TestProcessContext =>
        testRunner = new SbtTestInterfaceRunner(testContext)
        println("Set up testContext")
        SetupTestContextSuccessful()
      case Request.Empty => throw new MatchError(req)
    }

  def toTestResult(status: Status): Response =
    status match {
      case Status.Success => TestsSuccessful()
      case _              => TestsUnsuccessful()
    }

  def toInitialTestResult(status: Status, coverage: CoverageTestRunMap): Response =
    CoverageTestRunResult(status == Status.Success, Some(coverage))

}
