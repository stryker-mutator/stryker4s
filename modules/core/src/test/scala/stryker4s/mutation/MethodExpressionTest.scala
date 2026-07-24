package stryker4s.mutation

import stryker4s.testkit.Stryker4sSuite

class MethodExpressionTest extends Stryker4sSuite {
  test("ArgMethodExpression should match with a filter call (one argument)") {
    assertMatches("list.filter( _ => true )".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should match with an infix filter call (one argument)") {
    assertMatches("list filter( _ => true )".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should match with a filter call (block with one argument)") {
    assertMatches("list.filter { _ => true }".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should match with an infix filter call (block with one argument)") {
    assertMatches("list filter { _ => true }".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should match with a filter call (partial function)") {
    assertMatches("list.filter { case Foo(a, b) => true }".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should match with an infix filter call (partial function)") {
    assertMatches("list filter { case Foo(a, b) => true }".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should match with a filter call (aux function)") {
    assertMatches("list.filter(isBig)".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should match with an infix filter call (aux function)") {
    assertMatches("list filter(isBig)".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should not match with an infix filter call (non arguments)") {
    assertNoMatches("list filter()".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should not match with a filter call (more than one argument)") {
    assertNoMatches("list.filter((foo, bar) => true)".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should not match with an infix filter call (more than one argument)") {
    assertNoMatches("list filter((foo, bar) => true)".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should not match with a filter call (more than one argument) 2") {
    assertNoMatches("list.filter(foo, bar)".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should not match with an infix filter call (more than one argument) 2") {
    assertNoMatches("list filter(foo, bar)".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should not match with a filter call (block more than one argument)") {
    assertNoMatches("list.filter { (foo, bar) => true }".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should not match with a filter property") {
    assertNoMatches("foo.filter".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should not match with an infix filter property") {
    assertNoMatches("foo filter".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should not match with a filter import") {
    assertNoMatches("import foo.bar.filter.baz".parseStat) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should not match with a filter variable") {
    assertNoMatches("filter.foo".parseTerm) { case Filter(_, _) => true }
  }

  test("ArgMethodExpression should match exists to Exists") {
    assertMatches("list.exists(_ == 1)".parseTerm) { case Exists(_, _) => true }
  }

  test("ArgMethodExpression should match forall to Forall") {
    assertMatches("list.forall(_ == 1)".parseTerm) { case Forall(_, _) => true }
  }

  test("ArgMethodExpression should match take to Take") {
    assertMatches("list.take(1)".parseTerm) { case Take(_, _) => true }
  }

  test("ArgMethodExpression should match drop to Drop") {
    assertMatches("list.drop(1)".parseTerm) { case Drop(_, _) => true }
  }

  test("ArgMethodExpression should match takeRight to TakeRight") {
    assertMatches("list.takeRight(1)".parseTerm) { case TakeRight(_, _) => true }
  }

  test("ArgMethodExpression should match dropRight to DropRight") {
    assertMatches("list.dropRight(1)".parseTerm) { case DropRight(_, _) => true }
  }

  test("ArgMethodExpression should match takeWhile to TakeWhile") {
    assertMatches("list.takeWhile(_ < 2)".parseTerm) { case TakeWhile(_, _) => true }
  }

  test("ArgMethodExpression should match dropWhile to DropWhile") {
    assertMatches("list.dropWhile(_ < 2)".parseTerm) { case DropWhile(_, _) => true }
  }

  test("NoArgMethodExpression should not match with an isEmpty call (one argument)") {
    assertNoMatches("list.isEmpty( arg )".parseTerm) { case IsEmpty(_, _) => true }
  }

  test("NoArgMethodExpression should not match with an isEmpty call (block with one argument)") {
    assertNoMatches("list.isEmpty { arg }".parseTerm) { case IsEmpty(_, _) => true }
  }

  test("NoArgMethodExpression should not match with an isEmpty call (partial function)") {
    assertNoMatches("list.isEmpty { case Foo(a, b) => true }".parseTerm) { case IsEmpty(_, _) => true }
  }

  test("NoArgMethodExpression should not match with an isEmpty call (aux function)") {
    assertNoMatches("list.isEmpty(isBig)".parseTerm) { case IsEmpty(_, _) => true }
  }

  test("NoArgMethodExpression should not match with an isEmpty call (non arguments)") {
    assertNoMatches("list.isEmpty()".parseTerm) { case IsEmpty(_, _) => true }
  }

  test("NoArgMethodExpression should not match with an isEmpty call (more than one argument)") {
    assertNoMatches("list.isEmpty((foo, bar) => true)".parseTerm) { case IsEmpty(_, _) => true }
  }

  test("NoArgMethodExpression should not match with an isEmpty call (block more than one argument)") {
    assertNoMatches("list.isEmpty { (foo, bar) => true }".parseTerm) { case IsEmpty(_, _) => true }
  }

  test("NoArgMethodExpression should match with an isEmpty property") {
    assertMatches("foo.isEmpty".parseTerm) { case IsEmpty(_, _) => true }
  }

  test("NoArgMethodExpression should not match with an isEmpty import") {
    assertNoMatches("import foo.bar.isEmpty.baz".parseStat) { case IsEmpty(_, _) => true }
  }

  test("NoArgMethodExpression should not match with an isEmpty variable") {
    assertNoMatches("isEmpty.foo".parseTerm) { case IsEmpty(_, _) => true }
  }
}
