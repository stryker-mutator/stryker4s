package stryker4s.extensions.mutationtypes

import scala.meta.Term

case object GreaterThan extends EqualityOperator {
  override val tree: Term.Name = Term.Name(">")
}

case object GreaterThanEqualTo extends EqualityOperator {
  override val tree: Term.Name = Term.Name(">=")
}

case object LesserThanEqualTo extends EqualityOperator {
  override val tree: Term.Name = Term.Name("<=")
}

case object LesserThan extends EqualityOperator {
  override val tree: Term.Name = Term.Name("<")
}

case object EqualTo extends EqualityOperator {
  override val tree: Term.Name = Term.Name("==")
}

case object NotEqualTo extends EqualityOperator {
  override val tree: Term.Name = Term.Name("!=")
}
