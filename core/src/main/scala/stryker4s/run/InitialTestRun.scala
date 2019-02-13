package stryker4s.run
import better.files.File
import grizzled.slf4j.Logging
import stryker4s.extension.exception.InitialTestRunFailedException

trait InitialTestRun extends Logging {

  def initialTestRun(tmpDir: File): Unit = {
    info("Starting initial test run...")
    if (!runInitialTest(tmpDir)) {
      throw InitialTestRunFailedException(
        "Initial test run failed. Please make sure your tests pass before running Stryker4s."
      )
    }
    info("Initial test run succeeded! Testing mutants...")
  }

  def runInitialTest(workingDir: File): Boolean

}
