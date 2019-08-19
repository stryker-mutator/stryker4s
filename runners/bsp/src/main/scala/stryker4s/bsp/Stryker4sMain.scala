package stryker4s.bsp

import stryker4s.run.threshold.ErrorStatus

object Stryker4sMain extends App {

  val result = new Stryker4sBspRunner().run()

  val exitCode = result match {
    case ErrorStatus => 1
    case _           => 0
  }

  this.exit()

  private def exit(): Unit = {
    sys.exit(exitCode)
  }
}
