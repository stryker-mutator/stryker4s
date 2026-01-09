package stryker4s.mutants.tree

import cats.Monoid
import stryker4s.extension.TreeExtensions.*
import stryker4s.model.PlaceableTree
import stryker4s.mutants.TreeTraverser
import stryker4s.mutants.findmutants.MutantMatcher

import scala.meta.Tree

class MutantCollector(traverser: TreeTraverser, matcher: MutantMatcher) {

  def apply(tree: Tree): (Vector[IgnoredMutation], Map[PlaceableTree, Mutations]) = {

    // PartialFunction to check if the currently-visiting tree node is a node where we can place mutants
    val canPlaceF: PartialFunction[Tree, PlaceableTree] = Function.unlift(traverser.canPlace).andThen(PlaceableTree(_))

    // PartialFunction that _sometimes_ matches and returns the mutations at a PlaceableTree `NonEmptyList[Mutant]`
    val onEnterF = matcher.allMatchers.andThen(f => (p: PlaceableTree) => p -> f(p))

    // Walk through the tree and create a Map of PlaceableTree and Mutants
    val collected: Seq[(PlaceableTree, Either[IgnoredMutations, Mutations])] =
      tree.collectWithContext(canPlaceF)(onEnterF).reverse

    // IgnoredMutations are grouped by PlaceableTree, but we want all IgnoredMutations per file, which we can do with a combineAll
    Monoid.combineAll(
      collected.map {
        case (_, Left(m))  => (m.toVector, Map.empty[PlaceableTree, Mutations])
        case (p, Right(m)) => (Vector.empty, Map(p -> m))
      }
    )
  }
}
