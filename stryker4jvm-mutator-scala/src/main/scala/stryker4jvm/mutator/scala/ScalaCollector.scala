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

class ScalaCollector(var mutatorConfig: LanguageMutatorConfig) extends Collector[ScalaAST] {

  override def collect(ast: ScalaAST): CollectedMutants[ScalaAST] = {
    val tree = ast.tree;

    if (tree == null) {
      return null;
    }

    val traverser = new TraverserImpl()
    val matcher = new MutantMatcherImpl(config = mutatorConfig)

    var ignoredMutations: Vector[IgnoredMutation[ScalaAST]] = Vector()
    val mutations = Map[ScalaAST, ju.List[MutatedCode[ScalaAST]]]()

    def traverse(tree: Tree): Unit = {
      traverser.canPlace(tree) match {
        case Some(value) =>
          val res = matcher.allMatchers(value)

          if (res != null) {

            val ignored: Vector[IgnoredMutation[ScalaAST]] = Vector();
            val mutants: Vector[MutatedCode[ScalaAST]] = Vector();

            // val (ignored, mutants) = res.partitionMap(identity(_))
            // Doesn't exist in Scala 2.12 :(, so we get this ugly piece of code
            for (r <- res) {
              if (r.isLeft) {
                ignored :+ r.left.get;
              };

              if (r.isRight) {
                mutants :+ r.right.get;
              }
            }

            if (ignored.length > 0) {
              ignoredMutations = ignoredMutations ++ ignored
            }

            if (mutants.length > 0) {
              mutations + (new ScalaAST(term = value) -> mutants.asJava)
            }
          }
        case None => // Do nothing
      }

      tree.children.foreach(child => traverse(child))
    }

    traverse(tree)

    new CollectedMutants[ScalaAST](ignoredMutations.asJava, mutations.asJava)
  }

}
