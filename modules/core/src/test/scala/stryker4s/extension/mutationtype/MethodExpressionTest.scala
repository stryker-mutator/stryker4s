package stryker4s.extension.mutationtype

import stryker4s.testkit.Stryker4sSuite

import scala.meta.*

class MethodExpressionTest extends Stryker4sSuite {
  describe("ArgMethodExpression") {
    test("should match with a filter call (one argument)") {
      assertMatchPattern(q"list.filter( _ => true )", { case Filter(_, _) => })
    }

    test("should match with an infix filter call (one argument)") {
      assertMatchPattern(q"list filter( _ => true )", { case Filter(_, _) => })
    }

    test("should match with a filter call (block with one argument)") {
      assertMatchPattern(q"list.filter { _ => true }", { case Filter(_, _) => })
    }

    test("should match with an infix filter call (block with one argument)") {
      assertMatchPattern(q"list filter { _ => true }", { case Filter(_, _) => })
    }

    test("should match with a filter call (partial function)") {
      assertMatchPattern(q"list.filter { case Foo(a, b) => true }", { case Filter(_, _) => })
    }

    test("should match with an infix filter call (partial function)") {
      assertMatchPattern(q"list filter { case Foo(a, b) => true }", { case Filter(_, _) => })
    }

    test("should match with a filter call (aux function)") {
      assertMatchPattern(q"list.filter(isBig)", { case Filter(_, _) => })
    }

    test("should match with an infix filter call (aux function)") {
      assertMatchPattern(q"list filter(isBig)", { case Filter(_, _) => })
    }

    test("should not match with an infix filter call (non arguments)") {
      assertNotMatchPattern(q"list filter()", { case Filter(_, _) => })
    }

    test("should not match with a filter call (more than one argument)") {
      assertNotMatchPattern(q"list.filter((foo, bar) => true)", { case Filter(_, _) => })
    }

    test("should not match with an infix filter call (more than one argument)") {
      assertNotMatchPattern(q"list filter((foo, bar) => true)", { case Filter(_, _) => })
    }

    test("should not match with a filter call (more than one argument) 2") {
      assertNotMatchPattern(q"list.filter(foo, bar)", { case Filter(_, _) => })
    }

    test("should not match with an infix filter call (more than one argument) 2") {
      assertNotMatchPattern(q"list filter(foo, bar)", { case Filter(_, _) => })
    }

    test("should not match with a filter call (block more than one argument)") {
      assertNotMatchPattern(q"list.filter { (foo, bar) => true }", { case Filter(_, _) => })
    }

    test("should not match with a filter property") {
      assertNotMatchPattern(q"foo.filter", { case Filter(_, _) => })
    }

    test("should not match with an infix filter property") {
      assertNotMatchPattern(q"foo filter", { case Filter(_, _) => })
    }

    test("should not match with a filter import") {
      assertNotMatchPattern(q"import foo.bar.filter.baz", { case Filter(_, _) => })
    }

    test("should not match with a filter variable") {
      assertNotMatchPattern(q"filter.foo", { case Filter(_, _) => })
    }

    test("should match exists to Exists") {
      assertMatchPattern(q"list.exists(_ == 1)", { case Exists(_, _) => })
    }

    test("should match forall to Forall") {
      assertMatchPattern(q"list.forall(_ == 1)", { case Forall(_, _) => })
    }

    test("should match take to Take") {
      assertMatchPattern(q"list.take(1)", { case Take(_, _) => })
    }

    test("should match drop to Drop") {
      assertMatchPattern(q"list.drop(1)", { case Drop(_, _) => })
    }

    test("should match takeRight to TakeRight") {
      assertMatchPattern(q"list.takeRight(1)", { case TakeRight(_, _) => })
    }

    test("should match dropRight to DropRight") {
      assertMatchPattern(q"list.dropRight(1)", { case DropRight(_, _) => })
    }

    test("should match takeWhile to TakeWhile") {
      assertMatchPattern(q"list.takeWhile(_ < 2)", { case TakeWhile(_, _) => })
    }

    test("should match dropWhile to DropWhile") {
      assertMatchPattern(q"list.dropWhile(_ < 2)", { case DropWhile(_, _) => })
    }
  }

  describe("NoArgMethodExpression") {
    test("should not match with an isEmpty call (one argument)") {
      assertNotMatchPattern(q"list.isEmpty( arg )", { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty call (block with one argument)") {
      assertNotMatchPattern(q"list.isEmpty { arg }", { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty call (partial function)") {
      assertNotMatchPattern(q"list.isEmpty { case Foo(a, b) => true }", { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty call (aux function)") {
      assertNotMatchPattern(q"list.isEmpty(isBig)", { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty call (non arguments)") {
      assertNotMatchPattern(q"list.isEmpty()", { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty call (more than one argument)") {
      assertNotMatchPattern(q"list.isEmpty((foo, bar) => true)", { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty call (block more than one argument)") {
      assertNotMatchPattern(q"list.isEmpty { (foo, bar) => true }", { case IsEmpty(_, _) => })
    }

    test("should match with an isEmpty property") {
      assertMatchPattern(q"foo.isEmpty", { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty import") {
      assertNotMatchPattern(q"import foo.bar.isEmpty.baz", { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty variable") {
      assertNotMatchPattern(q"isEmpty.foo", { case IsEmpty(_, _) => })
    }
  }
}
