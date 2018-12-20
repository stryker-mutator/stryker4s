package stryker4s.extensions.exceptions
import scala.util.control.NoStackTrace

case class InitialTestRunFailedException(message: String) extends Exception(message) with NoStackTrace
