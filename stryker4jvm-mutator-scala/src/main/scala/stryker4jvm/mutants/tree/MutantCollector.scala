package stryker4jvm.mutants.tree

import cats.syntax.align.*
import stryker4jvm.extensions.TreeExtensions.*
import stryker4jvm.model.{IgnoredMutationReason, MutatedCode, PlaceableTree}
import stryker4jvm.mutants.Traverser
import stryker4jvm.mutants.findmutants.MutantMatcher

import scala.meta.Tree
import scala.meta.Term

class MutantCollector(
    traverser: Traverser,
    matcher: MutantMatcher
) {

  def apply(tree: Tree): (Vector[(MutatedCode[Term], IgnoredMutationReason)], Map[PlaceableTree, Mutations]) = {

    // PartialFunction to check if the currently-visiting tree node is a node where we can place mutants
    val canPlaceF: PartialFunction[Tree, PlaceableTree] = Function.unlift(traverser.canPlace).andThen(PlaceableTree(_))

    // PartialFunction that _sometimes_ matches and returns the mutations at a PlaceableTree `NonEmptyList[Mutant]`
    val onEnterF = matcher.allMatchers.andThen(f => (p: PlaceableTree) => p -> f(p))

    // Walk through the tree and create a Map of PlaceableTree and Mutants
    val collected: List[(PlaceableTree, Either[IgnoredMutations, Mutations])] =
      tree.collectWithContext(canPlaceF)(onEnterF)

    // IgnoredMutations are grouped by PlaceableTree, but we want all IgnoredMutations per file, which we can do with a foldLeft
    val (l, r) = collected.foldLeft(
      (Vector.newBuilder[(MutatedCode[Term], IgnoredMutationReason)], Map.empty[PlaceableTree, Mutations])
    ) {
      case ((acc, acc2), (_, Left(m)))  => (acc ++= m.toVector, acc2)
      case ((acc, acc2), (p, Right(m))) => (acc, acc2.alignCombine(Map(p -> m)))
    }
    l.result() -> r
  }
}
