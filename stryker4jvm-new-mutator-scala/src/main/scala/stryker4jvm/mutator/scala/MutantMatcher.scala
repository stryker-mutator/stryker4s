package stryker4jvm.mutator.scala

import cats.syntax.semigroup.*

import stryker4jvm.mutator.scala.extensions.PartialFunctionOps.*
import scala.meta.{Term, Tree}
import extensions.mutationtype.*

import stryker4jvm.core.model.{MutantMetaData, MutatedCode}

import stryker4jvm.core.model.CollectedMutants.IgnoredMutation
import stryker4jvm.mutator.scala.extensions.ImplicitMutationConversion.*

import stryker4jvm.mutator.scala.extensions.TreeExtensions.{LocationExtension, PositionExtension}

import MutantMatcher.MatcherResult

////
//// TODO: IGNORED MUTATIONS
////

trait MutantMatcher {
  def allMatchers: PartialFunction[Tree, Vector[MutatedCode[ScalaAST]]]
}

object MutantMatcher {
  type MatcherResult = PartialFunction[Tree, Either[IgnoredMutation[Tree], MutatedCode[Term]]]
}

class MutantMatcherImpl extends MutantMatcher {
  override def allMatchers: PartialFunction[Tree, Vector[MutatedCode[ScalaAST]]] =
    matchBooleanLiteral orElse
      matchEqualityOperator orElse
      matchLogicalOperator orElse
      matchConditionalExpression orElse
      matchMethodExpression orElse
      matchStringsAndRegex orElse
      test

  // Test method (temporary)
  def test: PartialFunction[Tree, Vector[MutatedCode[ScalaAST]]] = { case _ =>
    println("Found no mutations");
    null
  }

  def matchBooleanLiteral: PartialFunction[Tree, Vector[MutatedCode[ScalaAST]]] = {
    case True(orig)  => createMutations(orig)(False)
    case False(orig) => createMutations(orig)(True)
  }

  def matchEqualityOperator: PartialFunction[Tree, Vector[MutatedCode[ScalaAST]]] = {
    case GreaterThanEqualTo(orig) => createMutations(orig)(GreaterThan, LesserThan, EqualTo)
    case GreaterThan(orig)        => createMutations(orig)(GreaterThanEqualTo, LesserThan, EqualTo)
    case LesserThanEqualTo(orig)  => createMutations(orig)(LesserThan, GreaterThanEqualTo, EqualTo)
    case LesserThan(orig)         => createMutations(orig)(LesserThanEqualTo, GreaterThan, EqualTo)
    case EqualTo(orig)            => createMutations(orig)(NotEqualTo)
    case NotEqualTo(orig)         => createMutations(orig)(EqualTo)
    case TypedEqualTo(orig)       => createMutations(orig)(TypedNotEqualTo)
    case TypedNotEqualTo(orig)    => createMutations(orig)(TypedEqualTo)
  }

  def matchLogicalOperator: PartialFunction[Tree, Vector[MutatedCode[ScalaAST]]] = {
    case And(orig) => createMutations(orig)(Or)
    case Or(orig)  => createMutations(orig)(And)
  }

  def matchConditionalExpression: PartialFunction[Tree, Vector[MutatedCode[ScalaAST]]] = {
    case If(orig)      => createMutations(orig)(ConditionalTrue, ConditionalFalse)
    case While(orig)   => createMutations(orig)(ConditionalFalse)
    case DoWhile(orig) => createMutations(orig)(ConditionalFalse)
  }

  def matchMethodExpression: PartialFunction[Tree, Vector[MutatedCode[ScalaAST]]] = {
    case Filter(orig, f)      => createMutations(orig, f, FilterNot)
    case FilterNot(orig, f)   => createMutations(orig, f, Filter)
    case Exists(orig, f)      => createMutations(orig, f, Forall)
    case Forall(orig, f)      => createMutations(orig, f, Exists)
    case Take(orig, f)        => createMutations(orig, f, Drop)
    case Drop(orig, f)        => createMutations(orig, f, Take)
    case TakeRight(orig, f)   => createMutations(orig, f, DropRight)
    case DropRight(orig, f)   => createMutations(orig, f, TakeRight)
    case TakeWhile(orig, f)   => createMutations(orig, f, DropWhile)
    case DropWhile(orig, f)   => createMutations(orig, f, TakeWhile)
    case IsEmpty(orig, f)     => createMutations(orig, f, NonEmpty)
    case NonEmpty(orig, f)    => createMutations(orig, f, IsEmpty)
    case IndexOf(orig, f)     => createMutations(orig, f, LastIndexOf)
    case LastIndexOf(orig, f) => createMutations(orig, f, IndexOf)
    case Max(orig, f)         => createMutations(orig, f, Min)
    case Min(orig, f)         => createMutations(orig, f, Max)
    case MaxBy(orig, f)       => createMutations(orig, f, MinBy)
    case MinBy(orig, f)       => createMutations(orig, f, MaxBy)
  }

  /** Match both strings and regexes instead of stopping when one of them gives a match
    */
  def matchStringsAndRegex: PartialFunction[Tree, Vector[MutatedCode[ScalaAST]]] = matchStringLiteral combine matchRegex

  def matchStringLiteral: PartialFunction[Tree, Vector[MutatedCode[ScalaAST]]] = {
    case EmptyString(orig)         => createMutations(orig)(StrykerWasHereString)
    case NonEmptyString(orig)      => createMutations(orig)(EmptyString)
    case StringInterpolation(orig) => createMutations(orig)(EmptyString)
  }

  def matchRegex: PartialFunction[Tree, Vector[MutatedCode[ScalaAST]]] = {
    case RegexConstructor(orig)   => createMutations(orig, RegexMutations(orig))
    case RegexStringOps(orig)     => createMutations(orig, RegexMutations(orig))
    case PatternConstructor(orig) => createMutations(orig, RegexMutations(orig))
  }

  // Match statements
  private def createMutations[T <: Tree](
      original: Term,
      f: String => Term,
      mutated: MethodExpression
  ): Vector[MutatedCode[ScalaAST]] = {
    val replacements = Vector(mutated)
    buildMutations[MethodExpression](original, replacements, _(f))
  }

  // Regex
  private def createMutations[T <: Term](
      original: Term,
      mutated: Any
      //   mutated: Either[IgnoredMutation, NonEmptyVector[RegularExpression]]
  ): Vector[MutatedCode[ScalaAST]] = {
    // placeableTree =>
    // mutated
    //   .leftMap(NonEmptyVector.one(_))
    //   .flatMap(muts => buildMutations[RegularExpression](original, muts, _.tree)(placeableTree))
    println("B")
    println(mutated)

    null
  }

  // Other mutations
  private def createMutations[T <: Term](
      original: Term
  )(
      firstReplacement: SubstitutionMutation[T],
      restReplacements: SubstitutionMutation[T]*
  ): Vector[MutatedCode[ScalaAST]] = {
    val replacements = Vector(firstReplacement) ++ restReplacements.toVector
    buildMutations[SubstitutionMutation[T]](original, replacements, _.tree)
  }

  // TODO: DO IGNORED MUTATIONS SOMEHOW
  private def buildMutations[T <: Mutation[? <: Tree]](
      original: Term,
      replacements: Vector[T],
      mutationToTerm: T => Term
  ) = {
    println(s"Building mutations for $original")

    val mutations = replacements.map { replacement =>
      val location = replacement match {
        case r: RegularExpression => r.location
        case _                    => original.pos.toLocation
      }

      val tree: Tree = mutationToTerm(replacement)
      val metadata = new MutantMetaData(original.syntax, tree.syntax, replacement.mutationName, location.asJvmCore)

      // Return mutated code
      new MutatedCode(new ScalaAST(term = original), metadata)
    }

    mutations
  }

}
