package stryker4jvm.mutator.scala

import org.scalatest.funspec.AnyFunSpec
import stryker4jvm.mutator.scala.scalatest.FileUtil
import fs2.io.file.Path
import java.nio.file.NoSuchFileException
import scala.meta.*

class ScalaCollectorTest extends AnyFunSpec {

  describe("test") {
    it("test") {
      val col = new ScalaCollector()
      col.collect(new ScalaAST(tree = q"if (3 > 2) {}"))
    }

    it("test2") {
      val col = new ScalaCollector()
      col.collect(new ScalaAST(tree = q"class Foo { def foo = x >= 15; if (x == 15) {} }"))
    }
  }
}
