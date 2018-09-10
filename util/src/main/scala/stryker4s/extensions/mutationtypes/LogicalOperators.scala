package stryker4s.extensions.mutationtypes
import scala.meta.Term

case object And extends BinaryOperator {
  override val tree: Term.Name = Term.Name("&&")
}

case object Or extends BinaryOperator {
  override val tree: Term.Name = Term.Name("||")
}
