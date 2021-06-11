package stryker4s.mutants.findmutants

import cats.data.EitherT
import cats.syntax.semigroup.*
import stryker4s.config.Config
import stryker4s.extension.PartialFunctionOps.*
import stryker4s.extension.TreeExtensions.{GetMods, PathToRoot, TreeIsInExtension}
import stryker4s.extension.mutationtype.*
import stryker4s.model.{IgnoredMutationReason, Mutant, MutantId, MutationExcluded}

import scala.meta.*

class MutantMatcher()(implicit config: Config) {
  private val ids = Iterator.from(0)

  /** A PartialFunction that can match on a ScalaMeta tree and return a `List[Either[IgnoredMutationReason, Mutant]]`.
    *
    * If the result is a `Left`, it means a mutant was found, but ignored. The ADT
    * [[stryker4s.model.IgnoredMutationReason]] shows the possible reasons.
    */
  type MutationMatcher = PartialFunction[Tree, List[Either[IgnoredMutationReason, Mutant]]]

  def allMatchers: MutationMatcher =
    matchBooleanLiteral orElse
      matchEqualityOperator orElse
      matchLogicalOperator orElse
      matchConditionalExpression orElse
      matchMethodExpression orElse
      matchStringsAndRegex

  def matchBooleanLiteral: MutationMatcher = {
    case True(orig)  => orig ~~> False
    case False(orig) => orig ~~> True
  }

  def matchEqualityOperator: MutationMatcher = {
    case GreaterThanEqualTo(orig) => orig.~~>(GreaterThan, LesserThan, EqualTo)
    case GreaterThan(orig)        => orig.~~>(GreaterThanEqualTo, LesserThan, EqualTo)
    case LesserThanEqualTo(orig)  => orig.~~>(LesserThan, GreaterThanEqualTo, EqualTo)
    case LesserThan(orig)         => orig.~~>(LesserThanEqualTo, GreaterThan, EqualTo)
    case EqualTo(orig)            => orig ~~> NotEqualTo
    case NotEqualTo(orig)         => orig ~~> EqualTo
    case TypedEqualTo(orig)       => orig ~~> TypedNotEqualTo
    case TypedNotEqualTo(orig)    => orig ~~> TypedEqualTo
  }

  def matchLogicalOperator: MutationMatcher = {
    case And(orig) => orig ~~> Or
    case Or(orig)  => orig ~~> And
  }

  def matchConditionalExpression: MutationMatcher = {
    case If(condition)      => condition.~~>(ConditionalTrue, ConditionalFalse)
    case While(condition)   => condition ~~> ConditionalFalse
    case DoWhile(condition) => condition ~~> ConditionalFalse
  }

  def matchMethodExpression: MutationMatcher = {
    case Filter(orig, f)      => orig.~~>(f, FilterNot)
    case FilterNot(orig, f)   => orig.~~>(f, Filter)
    case Exists(orig, f)      => orig.~~>(f, Forall)
    case Forall(orig, f)      => orig.~~>(f, Exists)
    case Take(orig, f)        => orig.~~>(f, Drop)
    case Drop(orig, f)        => orig.~~>(f, Take)
    case TakeRight(orig, f)   => orig.~~>(f, DropRight)
    case DropRight(orig, f)   => orig.~~>(f, TakeRight)
    case TakeWhile(orig, f)   => orig.~~>(f, DropWhile)
    case DropWhile(orig, f)   => orig.~~>(f, TakeWhile)
    case IsEmpty(orig, f)     => orig.~~>(f, NonEmpty)
    case NonEmpty(orig, f)    => orig.~~>(f, IsEmpty)
    case IndexOf(orig, f)     => orig.~~>(f, LastIndexOf)
    case LastIndexOf(orig, f) => orig.~~>(f, IndexOf)
    case Max(orig, f)         => orig.~~>(f, Min)
    case Min(orig, f)         => orig.~~>(f, Max)
    case MaxBy(orig, f)       => orig.~~>(f, MinBy)
    case MinBy(orig, f)       => orig.~~>(f, MaxBy)
  }

  /** Match both strings and regexes instead of stopping when one of them gives a match
    */
  def matchStringsAndRegex: MutationMatcher = matchStringLiteral combine matchRegex

  def matchStringLiteral: MutationMatcher = {
    case EmptyString(orig)         => orig ~~> StrykerWasHereString
    case NonEmptyString(orig)      => orig ~~> EmptyString
    case StringInterpolation(orig) => orig ~~> EmptyString
  }

  def matchRegex: MutationMatcher = {
    case RegexConstructor(orig)   => orig ~~> RegexMutations(orig.value)
    case RegexStringOps(orig)     => orig ~~> RegexMutations(orig.value)
    case PatternConstructor(orig) => orig ~~> RegexMutations(orig.value)
  }

  implicit class TermExtensions(original: Term) {
    def ~~>[T <: Term](mutated: SubstitutionMutation[T]*): List[Either[IgnoredMutationReason, Mutant]] =
      createMutants[SubstitutionMutation[T]](mutated.toList, _.tree)

    def ~~>[T <: Term](
        mutated: Either[IgnoredMutationReason, Seq[SubstitutionMutation[T]]]
    ): List[Either[IgnoredMutationReason, Mutant]] = EitherT.fromEither[List](mutated).flatMapF(~~>(_*)).value

    def ~~>(f: String => Term, mutated: MethodExpression*): List[Either[IgnoredMutationReason, Mutant]] =
      createMutants[MethodExpression](mutated.toList, _(f))

    private def createMutants[T <: Mutation[? <: Tree]](
        mutations: List[T],
        mutationToTerm: T => Term
    ): List[Either[IgnoredMutationReason, Mutant]] =
      ifNotInAnnotation {
        mutations
          .map { mutated =>
            if (matchExcluded(mutated) || isSuppressedByAnnotation(mutated, original))
              Left(MutationExcluded)
            else {
              Right(
                Mutant(
                  MutantId(globalId = ids.next()),
                  original,
                  mutationToTerm(mutated),
                  mutated
                )
              )
            }
          }
      }

    private def ifNotInAnnotation(
        maybeMutants: => List[Either[IgnoredMutationReason, Mutant]]
    ): List[Either[IgnoredMutationReason, Mutant]] = {
      if (original.isIn[Mod.Annot])
        Nil
      else
        maybeMutants
    }

    private def matchExcluded(mutation: Mutation[?]): Boolean = {
      config.excludedMutations.contains(mutation.mutationName)
    }

    private def isSuppressedByAnnotation(mutation: Mutation[?], original: Term): Boolean = {

      original.pathToRoot.flatMap(_.getMods).exists(isSupressWarningsAnnotation(_, mutation))
    }

    private def isSupressWarningsAnnotation(mod: Mod, mutation: Mutation[?]) = {
      val mutationName = "stryker4s.mutation." + mutation.mutationName
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
}
