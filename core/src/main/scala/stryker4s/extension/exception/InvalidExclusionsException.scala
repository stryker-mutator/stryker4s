package stryker4s.extension.exception

import stryker4s.extension.mutationtype.Mutation

case class InvalidExclusionsException(invalid: Iterable[String]) extends Exception {
  override def getMessage: String = {
    s"""Invalid exclusion option(s): '${invalid.mkString(", ")}'
       |Valid exclusions are ${Mutation.mutations.mkString(", ")}.""".stripMargin
  }
}
