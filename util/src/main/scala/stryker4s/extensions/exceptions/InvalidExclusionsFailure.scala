package stryker4s.extensions.exceptions
import pureconfig.error.FailureReason
import stryker4s.extensions.mutationtypes.Mutation

case class InvalidExclusionsFailure(invalid: List[String]) extends FailureReason{
  override def description: String = {
    s"""Invalid exclusion option(s): '${invalid.mkString(", ")}'
       |Valid exclusions are ${Mutation.mutations.mkString(", ")}."""
      .stripMargin
  }
}
