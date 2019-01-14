package stryker4s.extensions.exceptions

import stryker4s.extensions.mutationtypes.Mutation

case class InvalidExclusionsException(invalid: Iterable[String]) extends Exception {
  override def getMessage: String = {
    s"""Invalid exclusion option(s): '${invalid.mkString(", ")}'
       |Valid exclusions are ${Mutation.mutations.mkString(", ")}.""".stripMargin
  }
}
