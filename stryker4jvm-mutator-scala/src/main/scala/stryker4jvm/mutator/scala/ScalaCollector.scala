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

class ScalaCollector(var mutatorConfig: LanguageMutatorConfig)(implicit log: Logger) extends Collector[ScalaAST] {

  override def collect(ast: ScalaAST): CollectedMutants[ScalaAST] = {
    val tree = ast.tree;

    if (tree == null) {
      return null;
    }

    val traverser = new TraverserImpl()
    val matcher = new MutantMatcherImpl(config = mutatorConfig)

    var ignoredMutations: Vector[IgnoredMutation[ScalaAST]] = Vector()
    var mutations = Map[ScalaAST, ju.List[MutatedCode[ScalaAST]]]()

    def traverse(tree: Tree): Unit = {

      println(tree);
      println(tree.getClass());
      println(s"Can place: ${traverser.canPlace(tree)}");
      println();

      traverser.canPlace(tree) match {
        case Some(value: Term) =>
          println(value.getClass())
          println(tree)

          val res = matcher.allMatchers(value)
          println(s"RES: $res")

          if (res != null) {

            var ignored: Vector[IgnoredMutation[ScalaAST]] = Vector();
            var mutants: Vector[MutatedCode[ScalaAST]] = Vector();

            // val (ignored, mutants) = res.partitionMap(identity(_))
            // Doesn't exist in Scala 2.12 :(, so we get this ugly piece of code
            for (r <- res) {
              if (r.isLeft) {
                ignored = ignored :+ r.left.get;
              };

              if (r.isRight) {
                mutants = mutants :+ r.right.get;
              }
            }

            if (ignored.length > 0) {
              ignoredMutations = ignoredMutations ++ ignored
            }

            if (mutants.length > 0) {
              mutations = mutations + (new ScalaAST(term = value) -> mutants.asJava)
            }
          }
        case None => // Do nothing
      }

      tree.children.foreach(child => traverse(child))
    }

    traverse(tree)

    // println(mutations)

    new CollectedMutants[ScalaAST](ignoredMutations.asJava, mutations.asJava)
  }

}
