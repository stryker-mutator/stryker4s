package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: forces the default branch of `getOrElse`.
  *
  * `maybeQuantity.getOrElse(0)` becomes `0`, so the value carried by a `Some`/`Right` is never used. This tests whether
  * a suite covers the present case at all, or only ever exercises the empty case where the default is returned anyway.
  *
  * Both the one-argument form (`Option`, `Either`, `IO`-like) and the two-argument `Map` form (`map.getOrElse(key,
  * default)`) are handled by always taking the last argument, which is the default in both. `getOrElseF` is covered as
  * well, since cats uses it for the effectful variant.
  */
class GetOrElseMutator extends CustomMutator {
  def matcher: MutationMatcher = {
    case term @ Term.Apply.After_4_6_0(Term.Select(_, Term.Name(name)), args)
        if isGetOrElse(name) && hasDefaultArgument(args) =>
      mutate(term, defaultArgument(args), name)
    case term @ Term.ApplyInfix.After_4_6_0(_, Term.Name(name), Type.ArgClause(Nil), args)
        if isGetOrElse(name) && hasDefaultArgument(args) =>
      mutate(term, defaultArgument(args), name)
  }

  private def isGetOrElse(name: String): Boolean =
    name == "getOrElse" || name == "getOrElseF"

  /** One argument for `Option`/`Either`, two for `Map` (`key` then `default`). */
  private def hasDefaultArgument(args: Term.ArgClause): Boolean =
    args.values.size == 1 || args.values.size == 2

  private def defaultArgument(args: Term.ArgClause): Term =
    args.values.last

  private def mutate(term: Term, replacement: Term, removed: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val metadata = MutantMetadata(removed, "default", "GetOrElse", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(term, replacement), metadata)))
  }
}
