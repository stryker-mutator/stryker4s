package stryker4jvm.mutator.scala

import stryker4jvm.core.model.Collector
import stryker4jvm.core.model.CollectedMutants
import stryker4jvm.core.model.CollectedMutants.IgnoredMutation

import stryker4jvm.mutator.scala.TraverserImpl
import scala.meta.{Term, Tree}

import stryker4jvm.core.model.MutatedCode
import stryker4jvm.core.config.LanguageMutatorConfig
import scala.collection.mutable.Map

import scala.collection.JavaConverters.*
import java.util as ju
import stryker4jvm.core.logging.Logger
import cats.syntax.align.*

import stryker4jvm.mutator.scala.extensions.TreeExtensions.*

class ScalaCollector(
    val traverser: Traverser,
    val matcher: MutantMatcher
)(implicit log: Logger)
    extends Collector[ScalaAST] {

  override def collect(ast: ScalaAST): CollectedMutants[ScalaAST] = {
    val tree = ast.value;

    if (tree == null) {
      return null;
    }

    // PartialFunction to check if the currently-visiting tree node is a node where we can place mutants
    val canPlaceF: PartialFunction[Tree, PlaceableTree] = Function.unlift(traverser.canPlace).andThen(PlaceableTree(_))

    // PartialFunction that _sometimes_ matches and returns the mutations at a PlaceableTree `NonEmptyList[Mutant]`
    val onEnterF = matcher.allMatchers.andThen(f => (p: PlaceableTree) => p -> f(p))

    // Walk through the tree and create a Map of PlaceableTree and Mutants
    val collected: List[(PlaceableTree, Either[Vector[IgnoredMutation[ScalaAST]], Vector[MutatedCode[ScalaAST]]])] =
      tree.collectWithContext(canPlaceF)(onEnterF)

    // Get mutations and ignoredmutations in correct format to return
    var ignoredMutations: Vector[IgnoredMutation[ScalaAST]] = Vector()
    var mutations = Map[ScalaAST, ju.List[MutatedCode[ScalaAST]]]()

    for (col <- collected) {
      val placeableTree = col._1
      col._2 match {
        case Left(value) => ignoredMutations = ignoredMutations ++ value
        case Right(mutants) =>
          val ast = new ScalaAST(value = placeableTree.tree);
          mutations = mutations + (ast -> mutants.asJava)
      }
    }

    new CollectedMutants[ScalaAST](ignoredMutations.asJava, mutations.asJava)
  }

}
