package stryker4jvm.mutator.scala

import scala.meta.quasiquotes.*
import stryker4jvm.mutator.scala.testutil.Stryker4jvmSuite
import scala.meta.Tree

class ScalaASTTest extends Stryker4jvmSuite {

  describe("AST") {
    it("Should have different hashcodes") {
      val ast: ScalaAST = new ScalaAST(value = q"x.foo()")
      val anotherAst: ScalaAST = new ScalaAST(value = q"x.foo()")

      ast.hashCode() shouldNot be(anotherAst.hashCode())
    }

    it("Should have the same syntax") {
      val ast: ScalaAST = new ScalaAST(value = q"x.foo()")
      val anotherAst: ScalaAST = new ScalaAST(value = q"x.foo()")

      ast.syntax() shouldBe anotherAst.syntax()
    }

    it("Should be equal to each other if the values are the same") {
      val v: Tree = q"x.foo()"
      val ast: ScalaAST = new ScalaAST(value = v)
      val anotherAst: ScalaAST = new ScalaAST(value = v)

      assert(ast.equals(anotherAst))
    }

    it("Should be equal to a tree with the same content") {
      val tree: Tree = q"x.foo()"
      val ast: ScalaAST = new ScalaAST(value = tree)

      assert(ast.equals(tree))
    }

    it("Should not equal an empty AST") {
      val ast: ScalaAST = new ScalaAST(value = q"x.foo()")
      val anotherAst: ScalaAST = new ScalaAST()

      assert(!ast.equals(anotherAst))
      assert(!anotherAst.equals(ast))
    }

    it("Should be an empty AST") {
      val ast: ScalaAST = new ScalaAST()

      ast.syntax() shouldBe "Undefined"
      ast.hashCode() shouldBe -1
    }
  }
}
