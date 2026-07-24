package stryker4s.mutation

import stryker4s.testkit.Stryker4sSuite

import scala.meta.*

class MutationTypesTest extends Stryker4sSuite {
  test("EqualityOperator > to GreaterThan") {
    assertMatches(Term.Name(">")) { case GreaterThan(_) => true }
  }

  test("EqualityOperator >= to GreaterThanEqualTo") {
    assertMatches(Term.Name(">=")) { case GreaterThanEqualTo(_) => true }
  }

  test("EqualityOperator <= to LesserThanEqualTo") {
    assertMatches(Term.Name("<=")) { case LesserThanEqualTo(_) => true }
  }

  test("EqualityOperator < to LesserThan") {
    assertMatches(Term.Name("<")) { case LesserThan(_) => true }
  }

  test("EqualityOperator == to EqualTo") {
    assertMatches(Term.Name("==")) { case EqualTo(_) => true }
  }

  test("EqualityOperator != to NotEqualTo") {
    assertMatches(Term.Name("!=")) { case NotEqualTo(_) => true }
  }

  test("BooleanLiteral false to False") {
    assertMatches(Lit.Boolean(false)) { case False(_) => true }
  }

  test("BooleanLiteral true to True") {
    assertMatches(Lit.Boolean(true)) { case True(_) => true }
  }

  test("LogicalOperator && to And") {
    assertMatches(Term.Name("&&")) { case And(_) => true }
  }

  test("LogicalOperator || to Or") {
    assertMatches(Term.Name("||")) { case Or(_) => true }
  }

  test("StringLiteral foo string to NonEmptyString") {
    assertMatches(Lit.String("foo")) { case NonEmptyString(_) => true }
  }

  test("StringLiteral empty string to EmptyString") {
    assertMatches(Lit.String("")) { case EmptyString(_) => true }
  }

  test("StringLiteral string interpolation to StringInterpolation") {
    assertMatches(
      Term.Interpolate(Term.Name("s"), List(Lit.String("foo "), Lit.String("")), List(Term.Name("foo")))
    ) { case StringInterpolation(_) => true }
  }

  test("StringLiteral q interpolation should not match StringInterpolation") {
    assertNoMatches(
      Term.Interpolate(Term.Name("q"), List(Lit.String("foo "), Lit.String("")), List(Term.Name("foo")))
    ) { case StringInterpolation(_) => true }
  }

  test("StringLiteral t interpolation should not match StringInterpolation") {
    assertNoMatches(
      Term.Interpolate(Term.Name("t"), List(Lit.String("scala.util.matching.Regex")), List.empty)
    ) { case StringInterpolation(_) => true }
  }

  test("other cases should return original tree on match") {
    val tree = Term.Name(">=")

    val result = GreaterThanEqualTo.unapply(tree).value

    assert(result eq tree)

  }

  test("other cases should convert GreaterThan to >") {
    val wrapped = WrappedTree(GreaterThan.tree)

    assertEquals(wrapped.term, Term.Name(">"))
  }

  test("other cases should convert to the proper type") {
    val falseTree: Tree = False.tree
    val greaterThan: Tree = GreaterThan.tree

    assertEquals(falseTree, Lit.Boolean(false))
    assertEquals(greaterThan, Term.Name(">"))
  }

}

final private case class WrappedTree(term: Tree)
