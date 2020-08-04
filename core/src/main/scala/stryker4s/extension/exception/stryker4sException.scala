package stryker4s.extension.exception

import scala.util.control.NoStackTrace

sealed abstract class Stryker4sException(message: String) extends Exception(message)

final case class UnableToBuildPatternMatchException() extends Stryker4sException("Unable to build pattern match")

final case class InitialTestRunFailedException(message: String) extends Stryker4sException(message) with NoStackTrace

final case class MutationRunFailedException(message: String) extends Stryker4sException(message)
