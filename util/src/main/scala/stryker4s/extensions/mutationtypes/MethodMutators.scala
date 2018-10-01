package stryker4s.extensions.mutationtypes

import scala.meta.{Term, Tree}

//case object Filter extends MethodMutator {
//  override val tree: Term.Name = Term.Name("filter")
//}

case object Filter extends MethodMutator {
  protected val methodName = "filter"
}

//case object FilterNot extends MethodMutator {
//  override val tree: Term.Name = Term.Name("filterNot")
//}

case object FilterNot extends MethodMutator {
  protected val methodName = "filterNot"
}

case object Exists extends MethodMutator {
  protected val methodName = "exists"
}

case object ForAll extends MethodMutator {
  protected val methodName = "forAll"
}

case object IsEmpty extends MethodMutator {
  protected val methodName = "isEmpty"
}

case object NonEmpty extends MethodMutator {
  protected val methodName = "nonEmpty"
}

case object IndexOf extends MethodMutator {
  protected val methodName = "indexOf"
}

case object LastIndexOf extends MethodMutator {
  protected val methodName = "lastIndexOf"
}

case object Max extends MethodMutator {
  protected val methodName = "max"
}

case object Min extends MethodMutator {
  protected val methodName = "min"
}
