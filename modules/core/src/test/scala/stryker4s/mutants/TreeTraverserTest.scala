package stryker4s.mutants

import cats.syntax.option.*
import stryker4s.extension.TreeExtensions.*
import stryker4s.testkit.{LogMatchers, Stryker4sSuite}

import scala.meta.*

class TreeTraverserTest extends Stryker4sSuite with LogMatchers {

  val traverser = new TreeTraverserImpl()

  describe("canPlace") {
    test("can not place inside case guards") {
      val code = """x.bar(2) match {
        case 1 if x.foo() => 1
      }""".parseTerm

      val caseGuard = code.find("x.foo()".parseTerm).value
      assertCannotPlaceInside(caseGuard)
    }

    test("can place in case body") {
      val code = """x.bar(2) match {
        case 1 if x.foo() => 3
      }""".parseTerm
      val body = code.find(Lit.Int(3)).value
      val result = traverser.canPlace(body).value
      assertEquals(result, body)
    }

    test("can not place inside annotations") {
      val code = """
      @SuppressWarnings(Array("stryker4s.mutation.MethodExpression"))
      val x = foo()
        """.parseStat
      val annotation = code.collectFirst { case t: Mod.Annot => t }.value.init

      assertCannotPlaceInside(annotation)
    }

    test("can not place inside deep term Term") {
      val code = s"""def bar = P(CharIn("0-9").rep(1).!) map (_.toInt)""".parseDef
      val foo = code.find(Lit.String("0-9")).value

      val placeAtFoo = traverser.canPlace(foo)
      assertEquals(placeAtFoo, none)
    }

    test("can place outside part of a Term") {
      val code = """def bar = P(CharIn("0-9").rep(1).!) map (_.toInt)""".parseDef
      val foo = code.find("P(CharIn(\"0-9\").rep(1).!) map (_.toInt)".parseTerm).value

      val placeAtFoo = traverser.canPlace(foo).value
      assertEquals(placeAtFoo, foo)
    }

    test("can not place inside type literals") {
      val code = "type Foo = \"Bar\" | \"Baz\"".parseStat
      assertCannotPlaceInside(code)
    }
  }

  /** Asserts that traverser cannot place inside _any_ node of the given tree
    */
  def assertCannotPlaceInside(t: Tree): Unit =
    t.traverse { case t =>
      val _ = assertEquals(traverser.canPlace(t), none)
    }

}
