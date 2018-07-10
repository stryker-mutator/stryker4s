package stryker4s.extensions.mutationtypes

import scala.meta.Lit

case object True extends LiteralMutation[Lit.Boolean] {
  override val tree: Lit.Boolean = Lit.Boolean(true)
}

case object False extends LiteralMutation[Lit.Boolean] {
  override val tree: Lit.Boolean = Lit.Boolean(false)
}

case object EmptyString extends LiteralMutation[Lit.String] {
  override val tree: Lit.String = Lit.String("")
}

case object StrykerWasHereString extends LiteralMutation[Lit.String] {
  override val tree: Lit.String = Lit.String("Stryker was here!")
}

/** Not a mutation, just an extractor for pattern matching
  */
case object NonEmptyString {
  def unapply(arg: Lit.String): Option[Lit.String] = Some(arg).filter(_.value.nonEmpty)
}
