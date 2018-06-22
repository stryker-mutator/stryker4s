package stryker4s.stubs

import better.files.File
import stryker4s.run.process.ProcessRunner

import scala.util.Try

class TestProcessRunner(returns: Try[Int]) extends ProcessRunner {
  val timesCalled: Iterator[Int] = Iterator.from(0)

  override def apply(command: String, workingDir: File, envVar: (String, String)): Try[Int] = {
    timesCalled.next()
    returns
  }
}