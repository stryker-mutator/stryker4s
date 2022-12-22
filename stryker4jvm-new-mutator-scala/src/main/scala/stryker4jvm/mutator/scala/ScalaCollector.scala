package stryker4jvm.mutator.scala

import stryker4jvm.core.model.Collector
import stryker4jvm.core.model.CollectedMutants
import stryker4jvm.core.model.CollectedMutants.IgnoredMutation

import stryker4jvm.mutator.scala.TraverserImpl
import scala.meta.{Term, Tree}

import stryker4jvm.core.model.MutatedCode
import scala.collection.mutable.Map

import scala.collection.JavaConverters.*
import java.util as ju

class ScalaCollector extends Collector[ScalaAST] {

  override def collect(ast: ScalaAST): CollectedMutants[ScalaAST] = {
    val tree = ast.tree;

    if (tree == null) {
      return null;
    }

    val traverser = new TraverserImpl()
    val matcher = new MutantMatcherImpl()

    val map = Map[ScalaAST, ju.List[MutatedCode[ScalaAST]]]()

    def traverse(tree: Tree): Unit = {
      traverser.canPlace(tree) match {
        case Some(value) =>
          println(s"\nValue: $value")
          val mutants = matcher.allMatchers(value)
          println(s"Mutants: $mutants")
          map.addOne((new ScalaAST(term = value), mutants.asJava))
        case None => // Do nothing
      }

      tree.children.foreach(child => traverse(child))
    }

    traverse(tree)

    println()
    println("Results:")
    println(map)

    // Doesn't work yet, TODO: make it work
    val ignoredMutations: ju.List[IgnoredMutation[ScalaAST]] = Vector().asJava

    new CollectedMutants[ScalaAST](ignoredMutations, map.asJava)
  }

}
