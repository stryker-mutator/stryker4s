package stryker4jvm.exception

import stryker4jvm.core.exception.Stryker4jvmException

import scala.util.control.NoStackTrace

final case class InitialTestRunFailedException(message: String) extends Stryker4jvmException(message) with NoStackTrace
