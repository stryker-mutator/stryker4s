package stryker4s.extension

import cats.syntax.option.*
import stryker4s.extension.TreeExtensions.*
import stryker4s.testkit.Stryker4sSuite

import scala.collection.mutable.ListBuffer
import scala.meta.*

class TreeExtensionsTest extends Stryker4sSuite {
  describe("isIn") {
    test("should be false for annotations") {
      val tree = q"""
        @SuppressWarnings(Array("stryker4s.mutation.MethodExpression"))
        def quantifierLong[A: P]: P[Quantifier] = ???
        """
      val subTree = tree.find(Lit.String("stryker4s.mutation.MethodExpression")).value

      assert(subTree.isIn[Mod.Annot])
    }
  }

  describe("find") {
    test("should find statement in simple tree") {
      val tree = q"val x = y >= 5"

      val result = tree.find(q">=").value

      assertEquals(result, q">=")
    }

    test("should find statement in large tree") {
      val tree =
        q"""def foo(list: List[Int], otherList: List[Int]) = {
        val firstResult = list
          .filter(_ % 2 == 0)
          .map(_ * 5)
          .reverse
        val secondResult = otherList
          .filter(_ >= 5)
          .map(_ * 3)
          .drop(5)
        (firstResult, secondResult)
      }"""

      val result = tree.find(q"_ * 5").value

      assertEquals(result, q"_ * 5")
    }

    test("should return none if statement is not in tree") {
      val tree = q"def four: Int = x < 5"

      val result = tree.find(q"x >= 5")

      assertEquals(result, none)
    }

    test("should still have parents when statement is found") {
      val original = q"x > 5"

      val result = original.find(q">").value

      assert(result.parent.value eq original)
    }
  }

  describe("transformOnce") {

    test("should transform does not recursively transform new subtree") {
      val sut = q"def foo = 5"

      val result = sut.transformOnce { case q"5" => q"5 + 1" }

      assertEquals(result, q"def foo = 5 + 1")
    }

    test("should transform both appearances in the tree only once") {
      val sut = q"def foo = 5 + 5"

      val result = sut.transformOnce { case q"5" => q"(5 * 2)" }

      assertEquals(result, q"def foo = (5 * 2) + (5 * 2)")
    }

    test("should return the same tree if no transformation is applied") {
      val sut = q"def foo = 5"

      val result = sut.transformOnce { case q"6" => q"6 + 1" }

      assert(result eq sut)
    }

    test("should transform a parsed string and have changed syntax") {
      val sut = "val x: Int = 5".parse[Stat].get

      val result = sut.transformOnce { case q"5" => q"6" }

      val expected = q"val x: Int = 6"
      assertEquals(result, expected)
      assertEquals(result.syntax, expected.syntax)
    }
  }

  describe("collectWithContext") {
    test("should collect all statements without context") {
      val tree = q"def foo = 5"

      val result = tree.collectWithContext { case _ => () } { case q"5" =>
        _ => 6
      }

      assertEquals(result.loneElement, 6)
    }

    test("should collect and pass context") {
      val tree = q"def foo = 5"
      var context = 0

      val result = tree.collectWithContext { case q"5" => context += 1; context } { case q"5" =>
        c =>
          assertEquals(c, 1)
          6
      }

      assertEquals(context, 1)
      assertEquals(result.loneElement, 6)
    }

    test("should only evaluate functions once") {
      val tree = q"def foo = 5"
      var context = 0
      var result = 5 // offset to have different comparisons

      tree.collectWithContext { case q"5" => context += 1; context } { case q"5" =>
        c =>
          assertEquals(c, 1)
          result += 1
      }

      assertEquals(context, 1)
      assertEquals(result, 6)
    }

    test("should not search upwards for context if one has already been found") {
      val tree = q"def foo = { 4 + 2 }"
      val context = ListBuffer.empty[Tree]

      tree.collectWithContext { case t => context += t; context } { case q"2" =>
        _ => ()
      }

      assertEquals(context.loneElement.syntax, "2")
    }

    test("should not call context-building function if no collector is found") {
      val tree = q"def foo = 5"
      var called = false

      tree.collectWithContext { case _ => called = true } { case q"6" => _ => 6 }

      assert(!called)
    }

    test("should call with older context if not found on the currently-visiting tree") {
      val tree = q"def foo = 5 + 2"
      var context = 0

      tree.collectWithContext { case q"5" => context += 1; context } { case q"5" =>
        c => assertEquals(c, 1)
      }

      assertEquals(context, 1)
    }

    test("should pass down each collector its own context") {
      val tree = q"""def foo = {
        1 + 2
        3 - 4
      }"""
      var calls = 0

      tree.collectWithContext {
        case t if t.syntax == "1 + 2" => "firstContext"
        case t if t.syntax == "3 - 4" => "secondContext"
      } {
        case q"1" =>
          c =>
            calls += 1
            assertEquals(c, "firstContext")
        case q"3" =>
          c =>
            calls += 1
            assertEquals(c, "secondContext")
      }

      assertEquals(calls, 2)
    }

    test("should not pass context from separate trees") {
      val tree = q"""def foo = {
            1 + 2
            3 - 4
          }"""
      var calls = 0

      tree.collectWithContext {
        // Only match context on the first statement
        case t if t.syntax == "1" => "firstContext"
      } {
        case q"1" =>
          c =>
            calls += 1
            assertEquals(c, "firstContext")
        case q"3" =>
          c => fail(s"Should not be called, context was $c")
      }
      assertEquals(calls, 1)
    }
  }
}
