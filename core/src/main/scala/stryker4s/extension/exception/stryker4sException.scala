package stryker4s.extension.exception

import scala.util.control.NoStackTrace

sealed abstract class Stryker4sException(message: String) extends Exception(message)

final case class UnableToBuildPatternMatchException() extends Stryker4sException("Unable to build pattern match")

final case class InitialTestRunFailedException(message: String) extends Stryker4sException(message) with NoStackTrace

final case class TestSetupException(name: String)
    extends Stryker4sException(
      s"Could not setup mutation testing environment. Unable to resolve project $name. This could be due to compile errors or misconfiguration of Stryker4s. See debug logs for more information."
    )

final case class MutationRunFailedException(message: String) extends Stryker4sException(message)
