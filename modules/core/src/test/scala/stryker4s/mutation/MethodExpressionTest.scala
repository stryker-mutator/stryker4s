package stryker4s.mutation

import stryker4s.testkit.Stryker4sSuite

class MethodExpressionTest extends Stryker4sSuite {
  describe("ArgMethodExpression") {
    test("should match with a filter call (one argument)") {
      assertMatchPattern("list.filter( _ => true )".parseTerm, { case Filter(_, _) => })
    }

    test("should match with an infix filter call (one argument)") {
      assertMatchPattern("list filter( _ => true )".parseTerm, { case Filter(_, _) => })
    }

    test("should match with a filter call (block with one argument)") {
      assertMatchPattern("list.filter { _ => true }".parseTerm, { case Filter(_, _) => })
    }

    test("should match with an infix filter call (block with one argument)") {
      assertMatchPattern("list filter { _ => true }".parseTerm, { case Filter(_, _) => })
    }

    test("should match with a filter call (partial function)") {
      assertMatchPattern("list.filter { case Foo(a, b) => true }".parseTerm, { case Filter(_, _) => })
    }

    test("should match with an infix filter call (partial function)") {
      assertMatchPattern("list filter { case Foo(a, b) => true }".parseTerm, { case Filter(_, _) => })
    }

    test("should match with a filter call (aux function)") {
      assertMatchPattern("list.filter(isBig)".parseTerm, { case Filter(_, _) => })
    }

    test("should match with an infix filter call (aux function)") {
      assertMatchPattern("list filter(isBig)".parseTerm, { case Filter(_, _) => })
    }

    test("should not match with an infix filter call (non arguments)") {
      assertNotMatchPattern("list filter()".parseTerm, { case Filter(_, _) => })
    }

    test("should not match with a filter call (more than one argument)") {
      assertNotMatchPattern("list.filter((foo, bar) => true)".parseTerm, { case Filter(_, _) => })
    }

    test("should not match with an infix filter call (more than one argument)") {
      assertNotMatchPattern("list filter((foo, bar) => true)".parseTerm, { case Filter(_, _) => })
    }

    test("should not match with a filter call (more than one argument) 2") {
      assertNotMatchPattern("list.filter(foo, bar)".parseTerm, { case Filter(_, _) => })
    }

    test("should not match with an infix filter call (more than one argument) 2") {
      assertNotMatchPattern("list filter(foo, bar)".parseTerm, { case Filter(_, _) => })
    }

    test("should not match with a filter call (block more than one argument)") {
      assertNotMatchPattern("list.filter { (foo, bar) => true }".parseTerm, { case Filter(_, _) => })
    }

    test("should not match with a filter property") {
      assertNotMatchPattern("foo.filter".parseTerm, { case Filter(_, _) => })
    }

    test("should not match with an infix filter property") {
      assertNotMatchPattern("foo filter".parseTerm, { case Filter(_, _) => })
    }

    test("should not match with a filter import") {
      assertNotMatchPattern("import foo.bar.filter.baz".parseStat, { case Filter(_, _) => })
    }

    test("should not match with a filter variable") {
      assertNotMatchPattern("filter.foo".parseTerm, { case Filter(_, _) => })
    }

    test("should match exists to Exists") {
      assertMatchPattern("list.exists(_ == 1)".parseTerm, { case Exists(_, _) => })
    }

    test("should match forall to Forall") {
      assertMatchPattern("list.forall(_ == 1)".parseTerm, { case Forall(_, _) => })
    }

    test("should match take to Take") {
      assertMatchPattern("list.take(1)".parseTerm, { case Take(_, _) => })
    }

    test("should match drop to Drop") {
      assertMatchPattern("list.drop(1)".parseTerm, { case Drop(_, _) => })
    }

    test("should match takeRight to TakeRight") {
      assertMatchPattern("list.takeRight(1)".parseTerm, { case TakeRight(_, _) => })
    }

    test("should match dropRight to DropRight") {
      assertMatchPattern("list.dropRight(1)".parseTerm, { case DropRight(_, _) => })
    }

    test("should match takeWhile to TakeWhile") {
      assertMatchPattern("list.takeWhile(_ < 2)".parseTerm, { case TakeWhile(_, _) => })
    }

    test("should match dropWhile to DropWhile") {
      assertMatchPattern("list.dropWhile(_ < 2)".parseTerm, { case DropWhile(_, _) => })
    }
  }

  describe("NoArgMethodExpression") {
    test("should not match with an isEmpty call (one argument)") {
      assertNotMatchPattern("list.isEmpty( arg )".parseTerm, { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty call (block with one argument)") {
      assertNotMatchPattern("list.isEmpty { arg }".parseTerm, { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty call (partial function)") {
      assertNotMatchPattern("list.isEmpty { case Foo(a, b) => true }".parseTerm, { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty call (aux function)") {
      assertNotMatchPattern("list.isEmpty(isBig)".parseTerm, { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty call (non arguments)") {
      assertNotMatchPattern("list.isEmpty()".parseTerm, { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty call (more than one argument)") {
      assertNotMatchPattern("list.isEmpty((foo, bar) => true)".parseTerm, { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty call (block more than one argument)") {
      assertNotMatchPattern("list.isEmpty { (foo, bar) => true }".parseTerm, { case IsEmpty(_, _) => })
    }

    test("should match with an isEmpty property") {
      assertMatchPattern("foo.isEmpty".parseTerm, { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty import") {
      assertNotMatchPattern("import foo.bar.isEmpty.baz".parseStat, { case IsEmpty(_, _) => })
    }

    test("should not match with an isEmpty variable") {
      assertNotMatchPattern("isEmpty.foo".parseTerm, { case IsEmpty(_, _) => })
    }
  }
}
