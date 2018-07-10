package stryker4s.extensions.mutationtypes

import scala.meta.{Term, Tree}

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

case object And extends TermNameMutation {
  override val tree: Term.Name = Term.Name("&&")
}

case object Or extends TermNameMutation {
  override val tree: Term.Name = Term.Name("||")
}

case object Filter extends TermNameMutation {
  override val tree: Term.Name = Term.Name("filter")
}

case object FilterNot extends TermNameMutation {
  override val tree: Term.Name = Term.Name("filterNot")
}

case object Exists extends TermNameMutation {
  override val tree: Term.Name = Term.Name("exists")
}

case object ForAll extends TermNameMutation {
  override val tree: Term.Name = Term.Name("forAll")
}

case object IsEmpty extends TermNameMutation {
  override val tree: Term.Name = Term.Name("isEmpty")
}

case object NonEmpty extends TermNameMutation {
  override val tree: Term.Name = Term.Name("nonEmpty")
}

case object IndexOf extends TermNameMutation {
  override val tree: Term.Name = Term.Name("indexOf")
}

case object LastIndexOf extends TermNameMutation {
  override val tree: Term.Name = Term.Name("lastIndexOf")
}

case object Max extends TermNameMutation {
  override val tree: Term.Name = Term.Name("max")
}

case object Min extends TermNameMutation {
  override val tree: Term.Name = Term.Name("min")
}
