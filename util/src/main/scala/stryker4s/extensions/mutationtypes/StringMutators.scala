package stryker4s.extensions.mutationtypes
import scala.meta.{Lit, Term}

case object EmptyString extends StringMutator[Lit.String] {
  override val tree: Lit.String = Lit.String("")

  override def unapply(arg: Lit.String): Option[Lit.String] =
    super.unapply(arg).filterNot(ParentIsInterpolatedString(_))
}

case object StrykerWasHereString extends StringMutator[Lit.String] {
  override val tree: Lit.String = Lit.String("Stryker was here!")
}

case object EmptyStringInterpolation extends StringMutator[Term.Interpolate] {
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

/** Not a mutation, just an extractor for pattern matching on interpolated strings
  */
case object StringInterpolation {
  import scala.meta.contrib.implicits.Equality._

  def unapply(arg: Term.Interpolate): Option[Term.Interpolate] =
    Some(arg).filter(_.prefix.isEqual(Term.Name("s"))) map wrapInterpolatedVariableInBlock

  /** Wrap a `Term.Name` args of a `Term.Interpolate` args in a `Term.Block` to work around a bug in Scalameta: https://github.com/scalameta/scalameta/issues/1792
    */
  private def wrapInterpolatedVariableInBlock(interpolation: Term.Interpolate): Term.Interpolate = {
    val Term.Interpolate(prefix, parts, args) = interpolation
    Term.Interpolate(prefix, parts, args.map {
      case t: Term.Name => Term.Block(List(t))
      case other        => other
    })
  }
}

private object ParentIsInterpolatedString {
  def apply(arg: Lit.String): Boolean = arg.parent match {
    // Do not mutate interpolated strings
    case Some(_: Term.Interpolate) => true
    case _                         => false
  }
}
