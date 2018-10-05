package stryker4s.extensions.mutationtypes

import scala.meta.Term

case object Filter extends MethodMutator {
  override val tree: Term.Name = Term.Name("filter")
}

case object FilterNot extends MethodMutator {
  override val tree: Term.Name = Term.Name("filterNot")
}

case object Exists extends MethodMutator {
  override val tree: Term.Name = Term.Name("exists")
}

case object ForAll extends MethodMutator {
  override val tree: Term.Name = Term.Name("forAll")
}

case object IsEmpty extends MethodMutator {
  override val tree: Term.Name = Term.Name("isEmpty")
}

case object NonEmpty extends MethodMutator {
  override val tree: Term.Name = Term.Name("nonEmpty")
}

case object IndexOf extends MethodMutator {
  override val tree: Term.Name = Term.Name("indexOf")
}

case object LastIndexOf extends MethodMutator {
  override val tree: Term.Name = Term.Name("lastIndexOf")
}

case object Max extends MethodMutator {
  override val tree: Term.Name = Term.Name("max")
}

case object Min extends MethodMutator {
  override val tree: Term.Name = Term.Name("min")
}
