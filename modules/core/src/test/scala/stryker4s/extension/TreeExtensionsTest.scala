package stryker4s.extension

import cats.syntax.eq.*
import cats.syntax.option.*
import mutationtesting.{Location, Position}
import stryker4s.extension.TreeExtensions.*
import stryker4s.testkit.Stryker4sSuite

import scala.collection.mutable.ListBuffer
import scala.meta.*

class TreeExtensionsTest extends Stryker4sSuite {
  describe("isIn") {
    test("should be false for annotations") {
      val tree = """
        @SuppressWarnings(Array("stryker4s.mutation.MethodExpression"))
        def quantifierLong[A: P]: P[Quantifier] = ???
        """.parseDef
      val subTree = tree.find(Lit.String("stryker4s.mutation.MethodExpression")).value

      assert(subTree.isIn[Mod.Annot])
    }
  }

  describe("find") {
    test("should find statement in simple tree") {
      val tree = "val x = y >= 5".parseStat

      val result = tree.find(Term.Name(">=")).value

      assertEquals(result, Term.Name(">="))
    }

    test("should find statement in large tree") {
      val tree =
        """def foo(list: List[Int], otherList: List[Int]) = {
        val firstResult = list
          .filter(_ % 2 == 0)
          .map(_ * 5)
          .reverse
        val secondResult = otherList
          .filter(_ >= 5)
          .map(_ * 3)
          .drop(5)
        (firstResult, secondResult)
      }""".parseDef

      val result = tree.find("_ * 5".parseTerm).value

      assertEquals(result, "_ * 5".parseTerm)
    }

    test("should return none if statement is not in tree") {
      val tree = "def four: Int = x < 5".parseDef

      val result = tree.find("x >= 5".parseTerm)

      assertEquals(result, none)
    }

    test("should still have parents when statement is found") {
      val original = "x > 5".parseTerm

      val result = original.find(Term.Name(">")).value

      assert(result.parent.value eq original)
    }
  }

  describe("treeEq") {
    test("equal for same instance") {
      val tree = "a + b".parseTerm
      assert(tree === tree)
    }

    test("equal for identical trees") {
      assert("a + b".parseTerm === "a + b".parseTerm)
    }

    test("equal ignoring positions") {
      assert("a + b".parseTerm === "a  +  b".parseTerm)
    }

    test("equal for nested trees") {
      assert("a + (b * c)".parseTerm === "a + (b * c)".parseTerm)
    }

    test("not equal for different node types") {
      assert("5".parseTerm =!= "x".parseTerm)
    }

    test("not equal for different name types") {
      assert((Term.Name("a"): Tree) =!= (Type.Name("a"): Tree))
    }

    test("not equal for different literal types") {
      assert("5".parseTerm =!= "\"5\"".parseTerm)
    }

    test("not equal for different name values") {
      assert(Term.Name("a") =!= Term.Name("b"))
    }

    test("not equal for different int literals") {
      assert(Lit.Int(5) =!= Lit.Int(6))
    }

    test("not equal for different string literals") {
      assert(Lit.String("a") =!= Lit.String("b"))
    }

    test("not equal for different boolean literals") {
      assert(Lit.Boolean(true) =!= Lit.Boolean(false))
    }

    test("not equal for different operators") {
      assert("a + b".parseTerm =!= "a - b".parseTerm)
    }

    test("not equal for different operands") {
      assert("a + b".parseTerm =!= "a + c".parseTerm)
    }

    test("not equal for different child count") {
      assert("f(a)".parseTerm =!= "f(a, b)".parseTerm)
    }

    test("not equal for nested difference") {
      assert("a + (b * c)".parseTerm =!= "a + (b * d)".parseTerm)
    }
  }

  describe("transformOnce") {

    test("should transform does not recursively transform new subtree") {
      val sut = "def foo = 5".parseDef

      val result = sut.transformOnce { case Lit.Int(5) => "5 + 1".parseTerm }

      assertEquals(result, "def foo = 5 + 1".parseDef)
    }

    test("should transform both appearances in the tree only once") {
      val sut = "def foo = 5 + 5".parseDef

      val result = sut.transformOnce { case Lit.Int(5) => "(5 * 2)".parseTerm }

      assertEquals(result, "def foo = (5 * 2) + (5 * 2)".parseDef)
    }

    test("should return the same tree if no transformation is applied") {
      val sut = "def foo = 5".parseDef

      val result = sut.transformOnce { case Lit.Int(6) => "6 + 1".parseTerm }

      assert(result eq sut)
    }

    test("should transform a parsed string and have changed syntax") {
      val sut = "val x: Int = 5".parse[Stat].get

      val result = sut.transformOnce { case Lit.Int(5) => Lit.Int(6) }

      val expected = "val x: Int = 6".parseStat
      assertEquals(result, expected)
      assertEquals(result.syntax, expected.syntax)
    }
  }

  describe("collectWithContext") {
    test("should collect all statements without context") {
      val tree = "def foo = 5".parseDef

      val result = tree.collectWithContext { case _ => () } { case Lit.Int(5) =>
        _ => 6
      }

      assertEquals(result.loneElement, 6)
    }

    test("should collect and pass context") {
      val tree = "def foo = 5".parseDef
      var context = 0

      val result = tree.collectWithContext { case Lit.Int(5) => context += 1; context } { case Lit.Int(5) =>
        c =>
          assertEquals(c, 1)
          6
      }

      assertEquals(context, 1)
      assertEquals(result.loneElement, 6)
    }

    test("should only evaluate functions once") {
      val tree = "def foo = 5".parseDef
      var context = 0
      var result = 5 // offset to have different comparisons

      val _ = tree.collectWithContext { case Lit.Int(5) => context += 1; context } { case Lit.Int(5) =>
        c =>
          assertEquals(c, 1)
          result += 1
      }

      assertEquals(context, 1)
      assertEquals(result, 6)
    }

    test("should not search upwards for context if one has already been found") {
      val tree = "def foo = { 4 + 2 }".parseDef
      val context = ListBuffer.empty[Tree]

      val _ = tree.collectWithContext { case t => context += t; context } { case Lit.Int(2) =>
        _ => ()
      }

      assertEquals(context.loneElement.syntax, "2")
    }

    test("should not call context-building function if no collector is found") {
      val tree = "def foo = 5".parseDef
      var called = false

      val _ = tree.collectWithContext { case _ => called = true } { case Lit.Int(6) => _ => 6 }

      assert(!called)
    }

    test("should call with older context if not found on the currently-visiting tree") {
      val tree = "def foo = 5 + 2".parseDef
      var context = 0

      val _ = tree.collectWithContext { case Lit.Int(5) => context += 1; context } { case Lit.Int(5) =>
        c => assertEquals(c, 1)
      }

      assertEquals(context, 1)
    }

    test("should pass down each collector its own context") {
      val tree = """def foo = {
        1 + 2
        3 - 4
      }""".parseDef
      var calls = 0

      val _ = tree.collectWithContext {
        case t if t.syntax == "1 + 2" => "firstContext"
        case t if t.syntax == "3 - 4" => "secondContext"
      } {
        case Lit.Int(1) =>
          c =>
            calls += 1
            assertEquals(c, "firstContext")
        case Lit.Int(3) =>
          c =>
            calls += 1
            assertEquals(c, "secondContext")
      }

      assertEquals(calls, 2)
    }

    test("should not pass context from separate trees") {
      val tree = """def foo = {
            1 + 2
            3 - 4
          }""".parseDef
      var calls = 0

      val _ = tree.collectWithContext {
        // Only match context on the first statement
        case t if t.syntax == "1" => "firstContext"
      } {
        case Lit.Int(1) =>
          c =>
            calls += 1
            assertEquals(c, "firstContext")
        case Lit.Int(3) =>
          c => fail(s"Should not be called, context was $c")
      }
      assertEquals(calls, 1)
    }

    test("should collect multiple values") {
      val tree = "def foo = 5 + 2".parseDef
      val result = tree.collectWithContext { case _ => () } { case Lit.Int(n) => _ => n }

      assertEquals(result, Seq(5, 2))
    }
  }

  describe("withOffset") {
    val wrxLocation = Location(Position(1, 2), Position(3, 4))
    val offset = Location(Position(5, 6), Position(7, 8))
    test("uses correct offset for regular string") {
      val result = wrxLocation.withOffset(offset, Lit.String("foo")) // single double-quote string "foo"

      assertEquals(result, mutationtesting.Location(mutationtesting.Position(6, 9), mutationtesting.Position(8, 11)))
    }

    test("uses correct offset for triple double-quote strings") {
      val result = wrxLocation.withOffset(
        offset,
        "\"\"\"foo\"\"\"".parse[Term].get.asInstanceOf[Lit.String]
      ) // triple double-quote string """foo"""

      assertEquals(result, mutationtesting.Location(mutationtesting.Position(6, 11), mutationtesting.Position(8, 13)))
    }
  }
}
