package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: removes collection ordering and de-duplication.
  *
  * `discounts.sorted`, `discounts.reverse`, `discounts.distinct`, `discounts.sortBy(f)`,
  * `discounts.sortWith(f)` and `discounts.distinctBy(f)` all become just `discounts`. The collection is still there;
  * only the guarantee about its order or uniqueness is gone.
  *
  * This targets a bug class that is easy to under-test because the input often happens to be sorted or duplicate-free
  * already. A test that builds its fixture in the order it expects back cannot tell whether the sort runs, and neither
  * can a test that never supplies a duplicate. Surviving mutants here point at exactly those tests.
  *
  * Note the asymmetry with [[HeadLastSwapMutator]]: this mutator *drops* a selection, so it must not match the function
  * of an enclosing `Term.Apply`. Dropping the selection of `discounts.sorted(ordering)` would leave
  * `discounts(ordering)`, which cannot compile. A mutator that only *renames* a selection has no such constraint.
  */
class CollectionOrderMutator extends CustomMutator {
  private def isAppliedCombinator(name: String): Boolean =
    name == "sortBy" || name == "sortWith" || name == "distinctBy"

  private def isNoArgCombinator(name: String): Boolean =
    name == "sorted" || name == "reverse" || name == "distinct"

  def matcher: MutationMatcher = {
    case term @ Term.Apply.After_4_6_0(Term.Select(receiver, Term.Name(name)), _) if isAppliedCombinator(name) =>
      mutate(term, receiver, name)
    case term @ Term.Select(receiver, Term.Name(name)) if isNoArgCombinator(name) && !isAppliedTo(term) =>
      mutate(term, receiver, name)
  }

  /** True when this term is the function of an enclosing call, as `discounts.sorted` is in `discounts.sorted(ordering)`
    * — in which case dropping it would strand the argument list.
    */
  private def isAppliedTo(term: Term): Boolean =
    term.parent.exists {
      case Term.Apply.After_4_6_0(fun, _) => fun eq term
      case _                              => false
    }

  private def mutate(term: Term, receiver: Term, removed: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val metadata = MutantMetadata(removed, "unordered", "CollectionOrder", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(term, receiver), metadata)))
  }
}
