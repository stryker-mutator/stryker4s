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
          statusToTestResult(status)
        } catch {
          case NonFatal(e) => ErrorDuringTestRun(e.getMessage())
        }

      case StartInitialTestRun() =>
        val status = testRunner.initialTestRun()
        statusToTestResult(status)
      case SetupTestContext(testContext) =>
        testRunner = new SbtTestInterfaceRunner(testContext)
        println("Set up testContext")
        SetupTestContextSuccesful()
    }

  def statusToTestResult(status: Status): TestResultResponse =
    status match {
      case Status.Success => TestsSuccessful()
      case _              => TestsUnsuccessful()
    }

}
