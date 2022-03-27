package stryker4s.mutants.tree

import cats.syntax.all.*
import stryker4s.extension.TreeExtensions.*
import stryker4s.model.{IgnoredMutationReason, MutatedCode, PlaceableTree}
import stryker4s.mutants.Traverser

import scala.collection.immutable.SortedMap
import scala.meta.Tree

class MutantCollector(
    traverser: Traverser
) {

  def apply(tree: Tree): (Vector[(MutatedCode, IgnoredMutationReason)], Map[PlaceableTree, Mutations]) = {
    // PartialFunction that always, and also checks if we can place a mutation on the currently-visiting Tree node

    val canPlaceF: PartialFunction[Tree, PlaceableTree] = Function.unlift(traverser.canPlace).andThen(PlaceableTree(_))

    // PartialFunction that _sometimes_ matches and returns the mutations at a PlaceableTree `NonEmptyList[Mutant]`
    val onEnterF = traverser.findMutations.andThen(f => (p: PlaceableTree) => p -> f(p))

    // Walk through the tree and create a Map of PlaceableTree and Mutants
    val collected: List[(PlaceableTree, Either[IgnoredMutations, Mutations])] =
      tree.collectWithContext(canPlaceF)(onEnterF)

    // IgnoredMutations are grouped by PlaceableTree, but we want all IgnoredMutations per file, which we can do with a foldLeft
    implicit val order = Ordering.by[PlaceableTree, String](_.toString)
    val (l, r) = collected.foldLeft(
      (Vector.newBuilder[(MutatedCode, IgnoredMutationReason)], SortedMap.empty[PlaceableTree, Mutations])
    ) {
      case ((acc, acc2), (_, Left(m)))  => (acc ++= m.toVector, acc2)
      case ((acc, acc2), (p, Right(m))) => (acc, acc2.alignCombine(SortedMap(p -> m)))
    }
    l.result() -> r.toMap

  }
}
