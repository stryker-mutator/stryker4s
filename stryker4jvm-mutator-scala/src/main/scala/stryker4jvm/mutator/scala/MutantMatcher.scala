package stryker4jvm.mutator.scala

import scala.meta.*

import MutantMatcher.MutationMatcher
import stryker4jvm.core.model.CollectedMutants.IgnoredMutation
import stryker4jvm.core.model.MutatedCode
import stryker4jvm.mutator.scala.PlaceableTree
import stryker4jvm.core.model.MutantMetaData

import cats.syntax.semigroup.*

import stryker4jvm.mutator.scala.extensions.PartialFunctionOps.*
import stryker4jvm.mutator.scala.extensions.TreeExtensions.{IsEqualExtension, PositionExtension, TransformOnceExtension}

import stryker4jvm.mutator.scala.extensions.mutationtype.*
import mutationtesting.Location
import stryker4jvm.core.model
import stryker4jvm.core.config.LanguageMutatorConfig

import scala.annotation.tailrec

trait MutantMatcher {

  /** Matches on all types of mutations and returns a list of all the mutations that were found.
    */
  def allMatchers: MutationMatcher
}

object MutantMatcher {

  /** A PartialFunction that can match on a ScalaMeta tree and return a `Either[IgnoredMutations, Mutations]`.
    *
    * If the result is a `Left`, it means a mutant was found, but ignored. The ADT
    * [[stryker4s.model.IgnoredMutationReason]] shows the possible reasons.
    */
  type MutationMatcher =
    PartialFunction[Tree, PlaceableTree => Either[Vector[IgnoredMutation[ScalaAST]], Vector[MutatedCode[ScalaAST]]]]

}

class MutantMatcherImpl(var config: LanguageMutatorConfig) extends MutantMatcher {

  override def allMatchers: MutationMatcher = {
    matchBooleanLiteral orElse
      matchEqualityOperator orElse
      matchLogicalOperator orElse
      matchConditionalExpression orElse
      matchMethodExpression orElse
      matchStringsAndRegex
  }

  def matchBooleanLiteral: MutationMatcher = {
    case True(orig)  => createMutations(orig)(False)
    case False(orig) => createMutations(orig)(True)
  }

  def matchEqualityOperator: MutationMatcher = {
    case GreaterThanEqualTo(orig) => createMutations(orig)(GreaterThan, LesserThan, EqualTo)
    case GreaterThan(orig)        => createMutations(orig)(GreaterThanEqualTo, LesserThan, EqualTo)
    case LesserThanEqualTo(orig)  => createMutations(orig)(LesserThan, GreaterThanEqualTo, EqualTo)
    case LesserThan(orig)         => createMutations(orig)(LesserThanEqualTo, GreaterThan, EqualTo)
    case EqualTo(orig)            => createMutations(orig)(NotEqualTo)
    case NotEqualTo(orig)         => createMutations(orig)(EqualTo)
    case TypedEqualTo(orig)       => createMutations(orig)(TypedNotEqualTo)
    case TypedNotEqualTo(orig)    => createMutations(orig)(TypedEqualTo)
  }

  def matchLogicalOperator: MutationMatcher = {
    case And(orig) => createMutations(orig)(Or)
    case Or(orig)  => createMutations(orig)(And)
  }

  def matchConditionalExpression: MutationMatcher = {
    case If(orig)      => createMutations(orig)(ConditionalTrue, ConditionalFalse)
    case While(orig)   => createMutations(orig)(ConditionalFalse)
    case DoWhile(orig) => createMutations(orig)(ConditionalFalse)
  }

  def matchMethodExpression: MutationMatcher = {
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

  def matchStringLiteral: MutationMatcher = {
    case EmptyString(orig)         => createMutations(orig)(StrykerWasHereString)
    case NonEmptyString(orig)      => createMutations(orig)(EmptyString)
    case StringInterpolation(orig) => createMutations(orig)(EmptyString)
  }

  def matchRegex: MutationMatcher = {
    case RegexConstructor(orig)   => createMutations(orig, RegexMutations(orig))
    case RegexStringOps(orig)     => createMutations(orig, RegexMutations(orig))
    case PatternConstructor(orig) => createMutations(orig, RegexMutations(orig))
  }

  /** Match both strings and regexes instead of stopping when one of them gives a match
    */
  def matchStringsAndRegex: MutationMatcher = matchStringLiteral combine matchRegex

  private def createMutations[T <: Term](
      original: Term
  )(
      firstReplacement: SubstitutionMutation[T],
      restReplacements: SubstitutionMutation[T]*
  ): PlaceableTree => Either[Vector[IgnoredMutation[ScalaAST]], Vector[MutatedCode[ScalaAST]]] = {
    val replacements: Vector[SubstitutionMutation[T]] =
      Vector(firstReplacement) ++ restReplacements.toVector
    buildMutations[SubstitutionMutation[T]](original, replacements, _.tree)
  }

  private def createMutations[T <: Tree](
      original: Term,
      f: String => Term,
      mutated: MethodExpression
  ): PlaceableTree => Either[Vector[IgnoredMutation[ScalaAST]], Vector[MutatedCode[ScalaAST]]] = {
    val replacements: Vector[MethodExpression] = Vector(mutated)
    buildMutations[MethodExpression](original, replacements, _(f))
  }

  private def createMutations[T <: Term](
      original: Term,
      mutated: Either[IgnoredMutation[ScalaAST], Vector[RegularExpression]]
  ): PlaceableTree => Either[Vector[IgnoredMutation[ScalaAST]], Vector[MutatedCode[ScalaAST]]] = { placeableTree =>
    import cats.syntax.either.*

    mutated
      .leftMap(Vector(_))
      .flatMap(muts => buildMutations[RegularExpression](original, muts, _.tree)(placeableTree))
  }

  private def buildMutations[T <: Mutation[? <: Tree]](
      original: Term,
      replacements: Vector[T],
      mutationToTerm: T => Term
  ): PlaceableTree => Either[Vector[IgnoredMutation[ScalaAST]], Vector[MutatedCode[ScalaAST]]] = placeableTree => {
    val mutations = replacements.map { replacement =>
      val location: model.elements.Location = replacement match {
        case r: RegularExpression =>
          val loc = r.location
          val start = loc.start
          val end = loc.end

          new model.elements.Location(
            new model.elements.Position(start.line, start.column),
            new model.elements.Position(end.line, end.column)
          )
        case _ =>
          val loc = original.pos.toLocation
          val start = loc.start
          val end = loc.end

          new model.elements.Location(
            new model.elements.Position(start.line, start.column),
            new model.elements.Position(end.line, end.column)
          )

      }

      val tree: Tree = mutationToTerm(replacement)
      val metadata = new MutantMetaData(original.syntax, tree.syntax, replacement.mutationName, location)
      val mutatedTopStatement = placeableTree.tree
        .transformExactlyOnce {
          case t if t.isEqual(original) && t.pos == original.pos =>
            tree
        }
        .getOrElse(
          throw new RuntimeException(
            s"Could not transform '$original' in ${placeableTree.tree} (${metadata.showLocation})"
          )
        )

      mutatedTopStatement match {
        case t: Term => new MutatedCode(new ScalaAST(value = t), metadata)
        case t =>
          throw new RuntimeException(
            s"Could not transform '$original' in ${placeableTree.tree} (${metadata.showLocation}). Expected a Term, but was a ${t.getClass().getSimpleName}"
          )
      }

    }

    filterExclusions(mutations, replacements.head, original)
  }

  private def filterExclusions(
      mutations: Vector[MutatedCode[ScalaAST]],
      mutationType: Mutation[?],
      original: Tree
  ): Either[Vector[IgnoredMutation[ScalaAST]], Vector[MutatedCode[ScalaAST]]] = {
    val mutationName = "stryker4s.mutation." + mutationType.mutationName

    if (excludedByConfig(mutationType.mutationName) || excludedByAnnotation(original, mutationName))
      Left(mutations.map(new IgnoredMutation[ScalaAST](_, new model.IgnoredMutationReason.MutationExcluded)))
    else
      Right(mutations)
  }

  private def excludedByConfig(mutation: String): Boolean = config.getExcludedMutations().contains(mutation)

  @tailrec
  private def excludedByAnnotation(original: Tree, mutationName: String): Boolean = {
    import stryker4jvm.mutator.scala.extensions.TreeExtensions.*
    original.parent match {
      case Some(value) =>
        value.getMods.exists(isSupressWarningsAnnotation(_, mutationName)) || excludedByAnnotation(
          value,
          mutationName
        )
      case None => false
    }
  }

  private def isSupressWarningsAnnotation(mod: Mod, mutationName: String): Boolean = {
    mod match {
      case Mod.Annot(Init(Type.Name("SuppressWarnings"), _, List(List(Term.Apply(Name("Array"), params))))) =>
        params.exists {
          case Lit.String(`mutationName`) => true
          case _                          => false
        }
      case _ => false
    }
  }
}
