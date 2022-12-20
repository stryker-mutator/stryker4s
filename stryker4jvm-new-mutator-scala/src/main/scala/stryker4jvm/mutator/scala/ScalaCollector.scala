package stryker4jvm.mutator.scala

import stryker4jvm.core.model.Collector
import stryker4jvm.core.model.CollectedMutants

import stryker4jvm.mutator.scala.Traverser
import scala.meta.Tree

class ScalaCollector extends Collector[ScalaAST] {

  override def collect(ast: ScalaAST): CollectedMutants[ScalaAST] = {
    val tree = ast.tree;

    if (tree == null) {
      return null;
    }

    val traverser = new TraverserImpl()

    def traverse(tree: Tree): Unit = {
      traverser.canPlace(tree) match {
        case Some(value) => println(value)
        case None        => // Nothing?
      }

      tree.children.foreach(child => traverse(child))
    }

    traverse(tree)

    null
  }

}
