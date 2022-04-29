package stryker4s.extension.mutationtype

import stryker4s.model.{MutantMetadata, MutatedCode, RegexParseError}
import stryker4s.mutants.tree.IgnoredMutation

import scala.meta.*
import scala.util.{Failure, Success}

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
  def apply(lit: Lit.String): Either[IgnoredMutation, Seq[RegularExpression]] = {
    val pattern = lit.value
    weaponregex.WeaponRegeX.mutate(pattern, mutationLevels = Seq(1)) match {
      case Failure(e) =>
        val metadata =
          MutatedCode(lit, MutantMetadata(pattern, "", new RegularExpression(pattern).mutationName, lit.pos))
        Left((metadata, RegexParseError(pattern, e)))
      case Success(value) => Right(value.map(r => RegularExpression(r.pattern)))
    }
  }
}

final case class RegularExpression(pattern: String) extends SubstitutionMutation[Lit.String] {

  def mutationName: String = classOf[RegularExpression].getSimpleName

  override def tree: Lit.String = Lit.String(pattern)

}
