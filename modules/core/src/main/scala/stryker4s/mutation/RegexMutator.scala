package stryker4s.mutation

import cats.data.NonEmptyVector
import cats.syntax.all.*
import mutationtesting.Location
import stryker4s.extension.TreeExtensions.{PositionExtension, RegexLocationExtension}
import stryker4s.model.{MutantMetadata, MutatedCode, RegexParseError}
import stryker4s.mutants.tree.IgnoredMutation

import scala.meta.*

/** Matches on `new scala.util.matching.Regex("[a-z]", _*)`
  */
case object RegexConstructor {
  // Two parents up is the full constructor
  def unapply(arg: Lit.String): Option[Lit.String] = arg.parent
    .flatMap(_.parent)
    .flatMap(_.parent)
    .collect {
      case Term.New(Init.After_4_6_0(Type.Name("Regex"), _, exprss))           => exprss
      case Term.New(Init.After_4_6_0(t"scala.util.matching.Regex", _, exprss)) => exprss
    }
    .collect { case Term.ArgClause(`arg` :: _, _) :: _ => arg }
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
  def unapply(arg: Lit.String): Option[Lit.String] = arg.parent.flatMap(_.parent).collect {
    case Term.Apply.After_4_6_0(q"Pattern.compile", Term.ArgClause(`arg` :: _, _))                 => arg
    case Term.Apply.After_4_6_0(q"java.util.regex.Pattern.compile", Term.ArgClause(`arg` :: _, _)) => arg
  }
}

object RegexMutations {
  def apply(lit: Lit.String): Either[IgnoredMutation, NonEmptyVector[RegularExpression]] = {
    weaponregex.WeaponRegeX
      .mutate(lit.value, mutationLevels = Seq(1))
      .leftMap(ignoredMutation(lit, _))
      .map(_.toVector)
      .map(
        NonEmptyVector
          .fromVectorUnsafe(_)
          .map(r => RegularExpression(r.pattern, r.location.toLocation(offset = lit.pos.toLocation), r.description))
      )
  }

  private def ignoredMutation(lit: Lit.String, e: String) = {
    val metadata =
      MutatedCode(lit, MutantMetadata(lit.value, "", "RegularExpression", lit.pos, none))
    (metadata, RegexParseError(lit.value, e))
  }
}

final case class RegularExpression(pattern: String, location: Location, description: String)
    extends SubstitutionMutation[Lit.String] {

  def mutationName: String = classOf[RegularExpression].getSimpleName
  override def fullName: String = classOf[RegularExpression].getName()

  override def tree: Lit.String = Lit.String(pattern)

}
