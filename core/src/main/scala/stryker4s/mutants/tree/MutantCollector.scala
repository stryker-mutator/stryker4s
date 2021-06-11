package stryker4s.mutants.tree

import cats.data.AndThen
import cats.syntax.functor.*
import cats.syntax.traverse.*
import stryker4s.model.{IgnoredMutationReason, MutatedCode, PlaceableTree}
import stryker4s.mutants.Traverser

import java.util.concurrent.atomic.AtomicReference
import scala.meta.{Term, Tree}

class MutantCollector(
    traverser: Traverser
) {

  def apply(tree: Tree): (Vector[(MutatedCode, IgnoredMutationReason)], Map[PlaceableTree, Mutations]) = {
    // PartialFunction that always, and also checks if we can place a mutation on the currently-visiting Tree node
    val canPlaceF: Tree => PlaceableTree = {
      val placeableTree = new AtomicReference[PlaceableTree](PlaceableTree(tree))

      tree => {
        Some(tree).collect { case t: Term => t }.flatMap(traverser.canPlace(_, placeableTree.get())).foreach { term =>
          placeableTree.set(PlaceableTree(term))
        }

        placeableTree.get()
      }
    }

    // PartialFunction that _sometimes_ matches and returns the mutations at a PlaceableTree `NonEmptyList[Mutant]`
    val onEnterF: Tree => Option[PlaceableTree => Either[IgnoredMutations, Mutations]] = traverser.findMutations.lift

    // Combine the two PF's
    // Some complex stuff with cats.data.AndThen to make sure the PF is only called once
    val collectF: PartialFunction[Tree, (PlaceableTree, Either[IgnoredMutations, Mutations])] = Function.unlift {
      (tree: Tree) =>
        AndThen(canPlaceF).andThen(p => onEnterF(tree).map(_(p)).tupleLeft(p)).apply(tree)
    }

    // Walk through the tree and create a Map of PlaceableTree and Mutants
    val collected: List[(PlaceableTree, Either[IgnoredMutations, Mutations])] = tree.collect(collectF)

    // IgnoredMutations are grouped by PlaceableTree, but we want all IgnoredMutations per file, which we can do with a traverse
    // TODO: is this correct?
    val (l, r) = collected
      .flatTraverse { case (placeableTree, resultPerMutation) =>
        resultPerMutation match {
          case Left(value)  => (value.toVector.toList, List.empty)
          case Right(value) => (List.empty, List(placeableTree -> value))
        }
      }
      .map(_.toMap)
    (l.toVector, r)
  }
}
