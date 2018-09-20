package stryker4s.extensions.mutationtypes
import scala.meta.Term

case object GreaterThan extends BinaryOperator {
  override val tree: Term.Name = Term.Name(">")
}

case object GreaterThanEqualTo extends BinaryOperator {
  override val tree: Term.Name = Term.Name(">=")
}

case object LesserThanEqualTo extends BinaryOperator {
  override val tree: Term.Name = Term.Name("<=")
}

case object LesserThan extends BinaryOperator {
  override val tree: Term.Name = Term.Name("<")
}

case object EqualTo extends BinaryOperator {
  override val tree: Term.Name = Term.Name("==")
}

case object NotEqualTo extends BinaryOperator {
  override val tree: Term.Name = Term.Name("!=")
}
