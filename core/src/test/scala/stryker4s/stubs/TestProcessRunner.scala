package stryker4s.stubs

import better.files.File
import stryker4s.run.process.ProcessRunner

import scala.util.Try

class TestProcessRunner(testRunExitCode: Try[Int]*) extends ProcessRunner {
  val timesCalled: Iterator[Int] = Iterator.from(0)

  /**
    * Keep track on the amount of times the function is called.
    * Also return an exit code which the test runner would do as well.
    */
  override def apply(command: String, workingDir: File, envVar: (String, String)): Try[Int] = {
    timesCalled.next()

    val testRun = envVar._2.toInt
    testRunExitCode(testRun)
  }
}
