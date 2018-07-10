package stryker4s.extensions.mutationtypes

import scala.meta.{Lit, Term}

case object True extends LiteralMutation[Lit.Boolean] {
  override val tree: Lit.Boolean = Lit.Boolean(true)
}

case object False extends LiteralMutation[Lit.Boolean] {
  override val tree: Lit.Boolean = Lit.Boolean(false)
}

case object EmptyString extends LiteralMutation[Lit.String] {
  override val tree: Lit.String = Lit.String("")

  override def unapply(arg: Lit.String): Option[Lit.String] =
    super.unapply(arg).filterNot(ParentIsInterpolatedString(_))
}

case object StrykerWasHereString extends LiteralMutation[Lit.String] {
  override val tree: Lit.String = Lit.String("Stryker was here!")
}

/** Not a mutation, just an extractor for pattern matching
  */
case object NonEmptyString {
  def unapply(arg: Lit.String): Option[Lit.String] =
    Some(arg).filter(_.value.nonEmpty).filterNot(ParentIsInterpolatedString(_))
}

object ParentIsInterpolatedString {
  def apply(arg: Lit.String): Boolean = arg.parent match {
    // Do not mutate interpolated strings
    case Some(_: Term.Interpolate) => true
    case _                         => false
  }
}
