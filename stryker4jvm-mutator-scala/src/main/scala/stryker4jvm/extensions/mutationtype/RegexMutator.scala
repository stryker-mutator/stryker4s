package stryker4jvm.extensions.mutationtype

import cats.data.NonEmptyVector
import cats.syntax.either.*
import mutationtesting.Location
import stryker4jvm.extensions.TreeExtensions.{PositionExtension, RegexLocationExtension}
import stryker4jvm.model.{MutantMetadata, RegexParseError}
import stryker4jvm.mutants.tree.IgnoredMutation

import scala.meta.*

import stryker4jvm.mutants.language.ScalaAST

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
  def apply(lit: Lit.String): Either[IgnoredMutation, NonEmptyVector[RegularExpression]] = {
    weaponregex.WeaponRegeX
      .mutate(lit.value, mutationLevels = Seq(1))
      .leftMap(ignoredMutation(lit, _))
      .map(_.toVector)
      .map(
        NonEmptyVector
          .fromVectorUnsafe(_)
          .map(r => RegularExpression(r.pattern, r.location.toLocation(offset = lit.pos.toLocation)))
      )
  }

  private def ignoredMutation(lit: Lit.String, e: String) = {
    val metadata =
      MutatedCode(
        lit.asInstanceOf[Term],
        MutantMetadata(lit.value, "", "RegularExpression", lit.pos.toLocation)
      )
    (metadata, RegexParseError(lit.value, e))
  }

}

final case class RegularExpression(pattern: String, location: Location) extends SubstitutionMutation[Lit.String] {

  def mutationName: String = classOf[RegularExpression].getSimpleName

  override def tree: Lit.String = Lit.String(pattern)

}
