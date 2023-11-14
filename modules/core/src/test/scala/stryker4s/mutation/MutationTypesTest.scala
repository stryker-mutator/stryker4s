package stryker4s.mutation

import stryker4s.extension.ImplicitMutationConversion.mutationToTree
import stryker4s.testkit.Stryker4sSuite

import scala.meta.*

class MutationTypesTest extends Stryker4sSuite {
  describe("EqualityOperator") {
    test("> to GreaterThan") {
      assertMatchPattern(q">", { case GreaterThan(_) => })
    }

    test(">= to GreaterThanEqualTo") {
      assertMatchPattern(q">=", { case GreaterThanEqualTo(_) => })
    }

    test("<= to LesserThanEqualTo") {
      assertMatchPattern(q"<=", { case LesserThanEqualTo(_) => })
    }

    test("< to LesserThan") {
      assertMatchPattern(q"<", { case LesserThan(_) => })
    }

    test("== to EqualTo") {
      assertMatchPattern(q"==", { case EqualTo(_) => })
    }

    test("!= to NotEqualTo") {
      assertMatchPattern(q"!=", { case NotEqualTo(_) => })
    }
  }

  describe("BooleanLiteral") {
    test("false to False") {
      assertMatchPattern(q"false", { case False(_) => })
    }

    test("true to True") {
      assertMatchPattern(q"true", { case True(_) => })
    }
  }

  describe("LogicalOperator") {
    test("&& to And") {
      assertMatchPattern(q"&&", { case And(_) => })
    }

    test("|| to Or") {
      assertMatchPattern(q"||", { case Or(_) => })
    }
  }

  describe("StringLiteral") {
    test("foo string to NonEmptyString") {
      assertMatchPattern(q""""foo"""", { case NonEmptyString(_) => })
    }

    test("empty string to EmptyString") {
      assertMatchPattern(Lit.String(""), { case EmptyString(_) => })
    }

    test("string interpolation to StringInterpolation") {
      assertMatchPattern(
        Term.Interpolate(q"s", List(Lit.String("foo "), Lit.String("")), List(q"foo")),
        { case StringInterpolation(_) => }
      )
    }

    test("q interpolation should not match StringInterpolation") {
      assertNotMatchPattern(
        Term.Interpolate(q"q", List(Lit.String("foo "), Lit.String("")), List(q"foo")),
        { case StringInterpolation(_) => }
      )
    }

    test("t interpolation should not match StringInterpolation") {
      assertNotMatchPattern(
        Term.Interpolate(q"t", List(Lit.String("scala.util.matching.Regex")), List.empty),
        { case StringInterpolation(_) => }
      )
    }
  }

  describe("other cases") {
    test("should return original tree on match") {
      val tree = q">="

      val GreaterThanEqualTo(result) = tree

      assert(result eq tree)

    }

    test("should convert GreaterThan to >") {
      val wrapped = WrappedTree(GreaterThan)

      assertEquals(wrapped.term, q">")
    }

    test("should convert to the proper type") {
      val falseTree: Tree = False
      val greaterThan: Tree = GreaterThan

      assertEquals(falseTree, q"false")
      assertEquals(greaterThan, q">")
    }

    final case class WrappedTree(term: Tree)
  }
}
