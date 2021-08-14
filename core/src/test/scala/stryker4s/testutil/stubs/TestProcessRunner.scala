package stryker4s.testutil.stubs

import cats.effect.IO
import fs2.io.file.Path
import stryker4s.log.Logger
import stryker4s.run.process.{Command, ProcessRunner}

import scala.util.{Success, Try}

object TestProcessRunner {
  def apply(testRunExitCode: Try[Int]*)(implicit log: Logger): TestProcessRunner =
    new TestProcessRunner(true, testRunExitCode: _*)
  def failInitialTestRun()(implicit log: Logger): TestProcessRunner = new TestProcessRunner(false)
}

class TestProcessRunner(initialTestRunSuccess: Boolean, testRunExitCode: Try[Int]*)(implicit log: Logger)
    extends ProcessRunner {
  val timesCalled: Iterator[Int] = Iterator.from(0)

  /** Keep track on the amount of times the function is called.
    *
    * Also return an exit code which the test runner would do as well.
    */
  override def apply(command: Command, workingDir: Path, envVar: (String, String)*): IO[Try[Int]] = {
    if (envVar.isEmpty) {
      IO.pure(Success(if (initialTestRunSuccess) 0 else 1))
    } else {
      timesCalled.next()
      IO.pure(testRunExitCode(envVar.map(_._2).head.toInt))
    }
  }
}
