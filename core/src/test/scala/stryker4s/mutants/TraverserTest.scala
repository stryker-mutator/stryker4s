package stryker4s.mutants

import stryker4s.extension.TreeExtensions.*
import stryker4s.model.PlaceableTree
import stryker4s.testutil.Stryker4sSuite

import scala.meta.Lit
import scala.meta.quasiquotes.*

class TraverserTest extends Stryker4sSuite {
  val traverser = new TraverserImpl()

  describe("canPlace") {
    it("can not place inside case guards") {
      val code = q"""x.bar(2) match {
        case 1 if x.foo() => 1
      }"""
      val startPattern = code.find(q"x.bar(2)").value
      val caseGuard = code.find(q"x.foo()").value
      val result = traverser.canPlace(caseGuard)
      result shouldBe None
    }

    it("can place in case body") {
      val code = q"""x.bar(2) match {
        case 1 if x.foo() => 3
      }"""
      val startPattern = code.find(q"x.bar(2)").value
      // val caseGuard = code.find(q"x.foo()").value
      val body = code.find(Lit.Int(3)).value
      val result = traverser.canPlace(body).value
      result shouldBe body
    }

    it("can place in case patterns") {
      val code = q"""for {
        refs <- x.bar(2)
        version <- r match {
          case Array(_, "pull", prNumber, _*) => foo
          case _                              => bar
        }
        if version.nonEmpty
      } yield version"""
      val startPattern = code.find(q"x.bar(2)").value

      val body = code.find(Lit.String("pull")).value
      val result = traverser.canPlace(body)
      result shouldBe None
    }
  }
}
