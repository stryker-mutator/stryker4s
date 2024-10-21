package stryker4s.mutation

import stryker4s.testkit.Stryker4sSuite

import scala.meta.*

class MutationTypesTest extends Stryker4sSuite {
  describe("EqualityOperator") {
    test("> to GreaterThan") {
      assertMatchPattern(Term.Name(">"), { case GreaterThan(_) => })
    }

    test(">= to GreaterThanEqualTo") {
      assertMatchPattern(Term.Name(">="), { case GreaterThanEqualTo(_) => })
    }

    test("<= to LesserThanEqualTo") {
      assertMatchPattern(Term.Name("<="), { case LesserThanEqualTo(_) => })
    }

    test("< to LesserThan") {
      assertMatchPattern(Term.Name("<"), { case LesserThan(_) => })
    }

    test("== to EqualTo") {
      assertMatchPattern(Term.Name("=="), { case EqualTo(_) => })
    }

    test("!= to NotEqualTo") {
      assertMatchPattern(Term.Name("!="), { case NotEqualTo(_) => })
    }
  }

  describe("BooleanLiteral") {
    test("false to False") {
      assertMatchPattern(Lit.Boolean(false), { case False(_) => })
    }

    test("true to True") {
      assertMatchPattern(Lit.Boolean(true), { case True(_) => })
    }
  }

  describe("LogicalOperator") {
    test("&& to And") {
      assertMatchPattern(Term.Name("&&"), { case And(_) => })
    }

    test("|| to Or") {
      assertMatchPattern(Term.Name("||"), { case Or(_) => })
    }
  }

  describe("StringLiteral") {
    test("foo string to NonEmptyString") {
      assertMatchPattern(Lit.String("foo"), { case NonEmptyString(_) => })
    }

    test("empty string to EmptyString") {
      assertMatchPattern(Lit.String(""), { case EmptyString(_) => })
    }

    test("string interpolation to StringInterpolation") {
      assertMatchPattern(
        Term.Interpolate(Term.Name("s"), List(Lit.String("foo "), Lit.String("")), List(Term.Name("foo"))),
        { case StringInterpolation(_) => }
      )
    }

    test("q interpolation should not match StringInterpolation") {
      assertNotMatchPattern(
        Term.Interpolate(Term.Name("q"), List(Lit.String("foo "), Lit.String("")), List(Term.Name("foo"))),
        { case StringInterpolation(_) => }
      )
    }

    test("t interpolation should not match StringInterpolation") {
      assertNotMatchPattern(
        Term.Interpolate(Term.Name("t"), List(Lit.String("scala.util.matching.Regex")), List.empty),
        { case StringInterpolation(_) => }
      )
    }
  }

  describe("other cases") {
    test("should return original tree on match") {
      val tree = Term.Name(">=")

      val result = GreaterThanEqualTo.unapply(tree).value

      assert(result eq tree)

    }

    test("should convert GreaterThan to >") {
      val wrapped = WrappedTree(GreaterThan.tree)

      assertEquals(wrapped.term, Term.Name(">"))
    }

    test("should convert to the proper type") {
      val falseTree: Tree = False.tree
      val greaterThan: Tree = GreaterThan.tree

      assertEquals(falseTree, Lit.Boolean(false))
      assertEquals(greaterThan, Term.Name(">"))
    }

    final case class WrappedTree(term: Tree)
  }
}
