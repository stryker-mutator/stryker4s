package stryker4s.extensions.mutationtypes

import scala.meta.{Lit, Term}
import scala.meta.contrib.implicits.Equality._

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

case object EmptyStringInterpolation extends Mutation[Term.Interpolate] {
  override val tree: Term.Interpolate = Term.Interpolate(Term.Name("s"), List(Lit.String("")), Nil)
}

/** Not a mutation, just an extractor for pattern matching on empty string
  */
case object NonEmptyString {
  def unapply(arg: Lit.String): Option[Lit.String] =
    Some(arg)
      .filter(_.value.nonEmpty)
      .filterNot(ParentIsInterpolatedString(_))
}

/** Not a mutation, just an extrator for pattern matching on interpolated strings
  */
case object StringInterpolation {
  def unapply(arg: Term.Interpolate): Option[Term.Interpolate] =
    Some(arg)
      .filter(_.prefix.isEqual(Term.Name("s")))
}

protected object ParentIsInterpolatedString {
  def apply(arg: Lit.String): Boolean = arg.parent match {
    // Do not mutate interpolated strings
    case Some(_: Term.Interpolate) => true
    case _                         => false
  }
}
