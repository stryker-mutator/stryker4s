package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*
import scala.meta.prettyprinters.XtensionReprint

/** Example custom mutator: replaces non-empty `List(...)`/`Seq(...)`/`Vector(...)` literal construction with the
  * type's `.empty` collection.
  *
  * Demonstrates the "Array/Collection Literal" mutator category that Stryker4s does not ship built-in, registered
  * here via the `strykerCustomMutators` sbt setting. Restricted to a small, explicit allow-list of collection
  * factory names to avoid false-positive matches on user-defined `apply` methods with the same name (this codebase
  * has no type information available at match time, so this syntactic restriction is the safest we can do).
  */
class CollectionLiteralMutator extends CustomMutator {
  private def isCollectionName(name: String): Boolean =
    name == "List" || name == "Seq" || name == "Vector"

  def matcher: MutationMatcher = {
    case term @ Term.Apply.After_4_6_0(Term.Name(name), Term.ArgClause(args, _))
        if isCollectionName(name) && args.nonEmpty =>
      mutate(term, name)
  }

  private def mutate(term: Term.Apply, collectionName: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val mutated = Term.Select(Term.Name(collectionName), Term.Name("empty"))
    val metadata = MutantMetadata(term.reprint(), mutated.reprint(), "CollectionLiteral", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(mutated, metadata)))
  }
}
