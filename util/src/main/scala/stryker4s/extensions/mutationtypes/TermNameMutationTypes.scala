package stryker4s.extensions.mutationtypes

import scala.meta.Term

case object GreaterThan extends TermNameMutation {
  override val tree: Term.Name = Term.Name(">")
}

case object GreaterThanEqualTo extends TermNameMutation {
  override val tree: Term.Name = Term.Name(">=")
}

case object LesserThanEqualTo extends TermNameMutation {
  override val tree: Term.Name = Term.Name("<=")
}

case object LesserThan extends TermNameMutation {
  override val tree: Term.Name = Term.Name("<")
}

case object EqualTo extends TermNameMutation {
  override val tree: Term.Name = Term.Name("==")
}

case object NotEqualTo extends TermNameMutation {
  override val tree: Term.Name = Term.Name("!=")
}

case object Filter extends TermNameMutation {
  override val tree: Term.Name = Term.Name("filter")
}

case object FilterNot extends TermNameMutation {
  override val tree: Term.Name = Term.Name("filterNot")
}
