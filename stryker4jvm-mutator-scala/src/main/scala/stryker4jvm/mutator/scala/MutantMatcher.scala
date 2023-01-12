package stryker4jvm.mutator.scala

import cats.syntax.semigroup.*

import stryker4jvm.mutator.scala.extensions.PartialFunctionOps.*
import scala.meta.{Term, Tree}
import extensions.mutationtype.*

import stryker4jvm.core.model.{MutantMetaData, MutatedCode}

import stryker4jvm.core.model.CollectedMutants.IgnoredMutation
import stryker4jvm.mutator.scala.extensions.ImplicitMutationConversion.*

import stryker4jvm.mutator.scala.extensions.TreeExtensions.{LocationExtension, PositionExtension}
import stryker4jvm.core.config.LanguageMutatorConfig

import MutantMatcher.MatcherResult
import stryker4jvm.core.model.IgnoredMutationReason

trait MutantMatcher {
  def allMatchers: PartialFunction[Tree, Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]]]
}

object MutantMatcher {
  type MatcherResult = PartialFunction[Tree, Either[IgnoredMutation[Tree], MutatedCode[Term]]]
}

class MutantMatcherImpl(var config: LanguageMutatorConfig) extends MutantMatcher {
  override def allMatchers: PartialFunction[Tree, Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]]] =
    matchBooleanLiteral orElse
      matchEqualityOperator orElse
      matchLogicalOperator orElse
      matchConditionalExpression orElse
      matchMethodExpression orElse
      matchStringsAndRegex orElse
      test

  // Test method (temporary)
  // TODO: It does something, no clue why it gets here
  def test: PartialFunction[Tree, Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]]] = { case orig =>
    println(orig.getClass());
    println("Found no mutations");
    null
  }

  def matchBooleanLiteral: PartialFunction[Tree, Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]]] = {
    case True(orig)  => createMutations(orig)(False)
    case False(orig) => createMutations(orig)(True)
  }

  def matchEqualityOperator: PartialFunction[Tree, Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]]] = {
    case GreaterThanEqualTo(orig) =>
      println("Komt hij hier?");
      createMutations(orig)(GreaterThan, LesserThan, EqualTo)
    case GreaterThan(orig)       => createMutations(orig)(GreaterThanEqualTo, LesserThan, EqualTo)
    case LesserThanEqualTo(orig) => createMutations(orig)(LesserThan, GreaterThanEqualTo, EqualTo)
    case LesserThan(orig)        => createMutations(orig)(LesserThanEqualTo, GreaterThan, EqualTo)
    case EqualTo(orig)           => createMutations(orig)(NotEqualTo)
    case NotEqualTo(orig)        => createMutations(orig)(EqualTo)
    case TypedEqualTo(orig)      => createMutations(orig)(TypedNotEqualTo)
    case TypedNotEqualTo(orig)   => createMutations(orig)(TypedEqualTo)
  }

  def matchLogicalOperator: PartialFunction[Tree, Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]]] = {
    case And(orig) => createMutations(orig)(Or)
    case Or(orig)  => createMutations(orig)(And)
  }

  def matchConditionalExpression
      : PartialFunction[Tree, Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]]] = {
    case If(orig)      => createMutations(orig)(ConditionalTrue, ConditionalFalse)
    case While(orig)   => createMutations(orig)(ConditionalFalse)
    case DoWhile(orig) => createMutations(orig)(ConditionalFalse)
  }

  def matchMethodExpression: PartialFunction[Tree, Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]]] = {
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
  def matchStringsAndRegex: PartialFunction[Tree, Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]]] =
    matchStringLiteral combine matchRegex

  def matchStringLiteral: PartialFunction[Tree, Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]]] = {
    case EmptyString(orig)         => createMutations(orig)(StrykerWasHereString)
    case NonEmptyString(orig)      => createMutations(orig)(EmptyString)
    case StringInterpolation(orig) => createMutations(orig)(EmptyString)
  }

  def matchRegex: PartialFunction[Tree, Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]]] = {
    case RegexConstructor(orig)   => createMutations(orig, RegexMutations(orig))
    case RegexStringOps(orig)     => createMutations(orig, RegexMutations(orig))
    case PatternConstructor(orig) => createMutations(orig, RegexMutations(orig))
  }

  // Match statements
  private def createMutations[T <: Tree](
      original: Term,
      f: String => Term,
      mutated: MethodExpression
  ): Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]] = {
    val replacements = Vector(mutated)
    buildMutations[MethodExpression](original, replacements, _(f))
  }

  // Regex
  private def createMutations[T <: Term](
      original: Term,
      mutated: Any
      //   mutated: Either[IgnoredMutation, NonEmptyVector[RegularExpression]]
  ): Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]] = {
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
  ): Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]] = {
    val replacements = Vector(firstReplacement) ++ restReplacements.toVector
    buildMutations[SubstitutionMutation[T]](original, replacements, _.tree)
  }

  private def buildMutations[T <: Mutation[? <: Tree]](
      original: Term,
      replacements: Vector[T],
      mutationToTerm: T => Term
  ): Vector[Either[IgnoredMutation[ScalaAST], MutatedCode[ScalaAST]]] = {
    replacements.map { replacement =>
      val location = replacement match {
        case r: RegularExpression => r.location
        case _                    => original.pos.toLocation
      }

      val tree: Tree = mutationToTerm(replacement)
      val metadata = new MutantMetaData(original.syntax, tree.syntax, replacement.mutationName, location.asJvmCore)
      val mutatedCode = new MutatedCode(new ScalaAST(term = original), metadata)

      if (config != null && config.getExcludedMutations().contains(replacement.mutationName)) {
        Left(new IgnoredMutation(mutatedCode, new ScalaReason))
      } else {
        Right(mutatedCode)
      }
    }
  }

}

class ScalaReason extends IgnoredMutationReason {

  override def explanation(): String = "test"

}
