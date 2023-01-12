package stryker4jvm.mutator.scala

import org.scalatest.funspec.AnyFunSpec
import stryker4jvm.mutator.scala.scalatest.FileUtil
import fs2.io.file.Path
import java.nio.file.NoSuchFileException
import stryker4jvm.core.config.LanguageMutatorConfig
import scala.meta.*
import scala.collection.JavaConverters.*
import java.util as ju

class ScalaCollectorTest extends AnyFunSpec {

  implicit val log = new ScalaLogger()
  val col = new ScalaCollector(new LanguageMutatorConfig(scala.collection.Set().asJava))

  describe("test") {
    it("test") {
      val tree = new ScalaAST(tree = q"def foo = 18 >= 20")
      val found = col.collect(tree);

      for (mutation <- found.mutations.asScala) {
        println(mutation._1.term)

        println(mutation._2)
        for (x <- mutation._2.asScala) {
          println(x)
          println(x.metaData.replacement)
        }
      }
    }

    // it("test2") {
    //   val found = col.collect(new ScalaAST(tree = q"class Foo { def foo = x >= 15; if (x == 15) {} }"));
    //   println("b")
    //   for (mutation <- found.mutations.asScala)
    //     println(mutation)
    // }

    // it("testExcluded") {
    //   val config = new LanguageMutatorConfig(Set("ConditionalExpression").asJava);
    //   val col = new ScalaCollector(config);

    //   val found = col.collect(new ScalaAST(tree = q"class Foo { def foo = x >= 15; if (x == 15) {} }"));
    //   println("c")
    //   for (mutation <- found.mutations.asScala)
    //     println(mutation)
    // }
  }

  // describe("allMatchers") {
  //   it("should match a conditional statement") {
  //     val tree = new ScalaAST(tree = q"def foo = 15 > 20 && 20 < 15");
  //     val found = col.collect(tree);

  //     // found.mutations.forEach()

  //     println("d")
  //     for (mutation <- found.mutations.asScala)
  //       println(mutation)

  //     // found.flatMap(_.toSeq).flatMap(_.toVector) should have length 7
  //     // expectMutations(found, q">", q">=", q"<", q"==")("EqualityOperator")
  //     // expectMutations(found, q"&&", q"||")("LogicalOperator")
  //     // expectMutations(found, q"<", q"<=", q">", q"==")("EqualityOperator")
  //   }
  // }
}
