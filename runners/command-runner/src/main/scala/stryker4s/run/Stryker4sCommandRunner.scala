package stryker4s.run

import stryker4s.run.threshold.ErrorStatus

object Stryker4sCommandRunner extends App {
  val result = new Stryker4sRunner().run()

  val exitCode = result match {
    case ErrorStatus => 1
    case _           => 0
  }
  this.exit()

  private def exit(): Unit = {
    sys.exit(exitCode)
  }
}
