package stryker4s.testutil.stubs

import better.files.File
import stryker4s.run.process.{Command, ProcessRunner}

import scala.util.{Success, Try}

object TestProcessRunner {
  def apply(testRunExitCode: Try[Int]*): TestProcessRunner = new TestProcessRunner(true, testRunExitCode: _*)
  def failInitialTestRun(): TestProcessRunner = new TestProcessRunner(false)
}

class TestProcessRunner(initialTestRunSuccess: Boolean, testRunExitCode: Try[Int]*) extends ProcessRunner {
  val timesCalled: Iterator[Int] = Iterator.from(0)

  /**
    * Keep track on the amount of times the function is called.
    * Also return an exit code which the test runner would do as well.
    */
  override def apply(command: Command, workingDir: File, envVar: (String, String)): Try[Int] = {
    if (envVar._2.equals("None")) {
      Success(if (initialTestRunSuccess) { 0 } else { 1 })
    } else {
      timesCalled.next()
      testRunExitCode(envVar._2.toInt)
    }
  }
}
