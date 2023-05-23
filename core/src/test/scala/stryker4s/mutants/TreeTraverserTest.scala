package stryker4s.mutants

import stryker4s.extension.TreeExtensions.*
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

import scala.meta.Lit
import scala.meta.quasiquotes.*

class TreeTraverserTest extends Stryker4sSuite with LogMatchers {

  val traverser = new TreeTraverserImpl()

  describe("canPlace") {
    it("can not place inside case guards") {
      val code = q"""x.bar(2) match {
        case 1 if x.foo() => 1
      }"""

      val caseGuard = code.find(q"x.foo()").value
      val result = traverser.canPlace(caseGuard)
      result shouldBe None
    }

    it("can place in case body") {
      val code = q"""x.bar(2) match {
        case 1 if x.foo() => 3
      }"""
      val body = code.find(Lit.Int(3)).value
      val result = traverser.canPlace(body).value
      result shouldBe body
    }

    it("can not place inside annotations") {
      val code = q"""
      @SuppressWarnings(Array("stryker4s.mutation.MethodExpression"))
      val x = foo()
        """

      val annotation = code.find(Lit.String("stryker4s.mutation.MethodExpression")).value
      val result = traverser.canPlace(annotation)
      result shouldBe None
    }

    it("can not place inside deep term Term") {
      val code = q"def bar = P(CharIn(${Lit.String("0-9")}).rep(1).!) map (_.toInt)"
      val foo = code.find(Lit.String("0-9")).value

      val placeAtFoo = traverser.canPlace(foo)
      placeAtFoo shouldBe None
    }

    it("can place outside part of a Term") {
      val code = q"def bar = P(CharIn(${Lit.String("0-9")}).rep(1).!) map (_.toInt)"
      val foo = code.find(q"P(CharIn(${Lit.String("0-9")}).rep(1).!)").value

      val placeAtFoo = traverser.canPlace(foo).value
      placeAtFoo shouldBe foo
    }
  }
}
