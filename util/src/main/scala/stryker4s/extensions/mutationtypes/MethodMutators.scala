package stryker4s.extensions.mutationtypes

case object Filter extends OneArgMethodMutator {
  protected val methodName = "filter"
}

case object FilterNot extends OneArgMethodMutator {
  protected val methodName = "filterNot"
}

case object Exists extends OneArgMethodMutator {
  protected val methodName = "exists"
}

case object ForAll extends OneArgMethodMutator {
  protected val methodName = "forAll"
}

case object Take extends OneArgMethodMutator {
  protected val methodName = "take"
}

case object Drop extends OneArgMethodMutator {
  protected val methodName = "drop"
}

case object IsEmpty extends NonArgsMethodMutator {
  protected val methodName = "isEmpty"
}

case object NonEmpty extends NonArgsMethodMutator {
  protected val methodName = "nonEmpty"
}

case object IndexOf extends OneArgMethodMutator {
  protected val methodName = "indexOf"
}

case object LastIndexOf extends OneArgMethodMutator {
  protected val methodName = "lastIndexOf"
}

case object Max extends NonArgsMethodMutator {
  protected val methodName = "max"
}

case object Min extends NonArgsMethodMutator {
  protected val methodName = "min"
}

case object MaxBy extends OneArgMethodMutator {
  protected val methodName = "maxBy"
}

case object MinBy extends OneArgMethodMutator {
  protected val methodName = "minBy"
}
