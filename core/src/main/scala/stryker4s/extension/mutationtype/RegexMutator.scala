package stryker4s.extension.mutationtype

import scala.meta.{Init, Term, _}
import scala.util.{Failure, Success}

import stryker4s.model.RegexParseError

/** Matches on `new scala.util.matching.Regex("[a-z]", _*)`
  */
case object RegexConstructor {
  // Two parents up is the full constructor
  def unapply(arg: Lit.String): Option[Lit.String] = arg.parent
    .flatMap(_.parent)
    .collect {
      case Term.New(Init(Type.Name("Regex"), _, exprss))           => exprss
      case Term.New(Init(t"scala.util.matching.Regex", _, exprss)) => exprss
    }
    .collect { case (`arg` :: _) :: _ => arg }
}

/** Matches on `"[a-z]".r`
  */
case object RegexStringOps {

  def unapply(arg: Lit.String): Option[Lit.String] = arg.parent
    .collect { case Term.Select(`arg`, Term.Name("r")) => arg }

}

/** Matches on `Pattern.compile("[a-z]", _*)`
  */
case object PatternConstructor {
  def unapply(arg: Lit.String): Option[Lit.String] = arg.parent.collect {
    case Term.Apply(q"Pattern.compile", `arg` :: _)                 => arg
    case Term.Apply(q"java.util.regex.Pattern.compile", `arg` :: _) => arg
  }
}

object RegexMutations {
  def apply(pattern: String): Either[RegexParseError, Seq[RegularExpression]] = {
    weaponregex.WeaponRegeX.mutate(pattern, mutationLevels = Seq(1)) match {
      case Failure(e)     => Left(RegexParseError(pattern, e))
      case Success(value) => Right(value.map(r => RegularExpression(r.pattern)))
    }
  }
}

final case class RegularExpression(pattern: String) extends SubstitutionMutation[Lit.String] {

  def mutationName: String = classOf[RegularExpression].getSimpleName

  override def tree: Lit.String = Lit.String(pattern)

}
