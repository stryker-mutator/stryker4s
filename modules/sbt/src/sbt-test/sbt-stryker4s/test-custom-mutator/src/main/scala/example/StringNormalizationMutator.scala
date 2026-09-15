package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: removes string normalization.
  *
  * `code.trim`, `code.strip`, `code.toLowerCase`, `code.toUpperCase`, `code.capitalize`, `code.stripPrefix(p)` and
  * `code.stripSuffix(s)` all become just `code`.
  *
  * Normalization is usually defensive: it exists for the inputs a test fixture rarely bothers to produce. A test that
  * only ever passes `"ABC"` cannot tell whether `toUpperCase` runs, and one that never passes a padded string cannot
  * tell whether `trim` runs. A surviving mutant here is a direct instruction to add the untidy input.
  *
  * Like [[CollectionOrderMutator]] this mutator *drops* a selection, so it must not match the function of an enclosing
  * `Term.Apply`.
  */
class StringNormalizationMutator extends CustomMutator {
  private def isAppliedCombinator(name: String): Boolean =
    name == "stripPrefix" || name == "stripSuffix"

  private def isNoArgCombinator(name: String): Boolean =
    name == "trim" || name == "strip" || name == "toLowerCase" || name == "toUpperCase" || name == "capitalize"

  def matcher: MutationMatcher = {
    case term @ Term.Apply.After_4_6_0(Term.Select(receiver, Term.Name(name)), _) if isAppliedCombinator(name) =>
      mutate(term, receiver, name)
    case term @ Term.Select(receiver, Term.Name(name)) if isNoArgCombinator(name) && !isAppliedTo(term) =>
      mutate(term, receiver, name)
  }

  private def isAppliedTo(term: Term): Boolean =
    term.parent.exists {
      case Term.Apply.After_4_6_0(fun, _) => fun eq term
      case _                              => false
    }

  private def mutate(term: Term, receiver: Term, removed: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val metadata = MutantMetadata(removed, "unnormalized", "StringNormalization", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(term, receiver), metadata)))
  }
}
