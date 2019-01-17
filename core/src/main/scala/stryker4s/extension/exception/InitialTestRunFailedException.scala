package stryker4s.extension.exception

import scala.util.control.NoStackTrace

case class InitialTestRunFailedException(message: String) extends Exception(message) with NoStackTrace
