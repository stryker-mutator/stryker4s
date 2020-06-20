package stryker4s.extension.exception
import stryker4s.extension.mutationtype.Mutation

import scala.util.control.NoStackTrace

sealed abstract class Stryker4sException(message: String) extends Exception(message)

final case class UnableToBuildPatternMatchException() extends Stryker4sException("Unable to build pattern match")

final case class InitialTestRunFailedException(message: String) extends Stryker4sException(message) with NoStackTrace

final case class TestSetupException(message: String) extends Stryker4sException(message)

final case class MutationRunFailedException(message: String) extends Stryker4sException(message)

final case class InvalidThresholdValueException(message: String) extends Stryker4sException(message)

final case class InvalidExclusionsException(invalid: Iterable[String])
    extends Stryker4sException(
      s"""Invalid exclusion option(s): '${invalid.mkString(", ")}'
         |Valid exclusions are ${Mutation.mutations.mkString(", ")}""".stripMargin
    )
