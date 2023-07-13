package stryker4s.extension.mutationtype

import stryker4s.testutil.Stryker4sSuite

import scala.meta.*

class MethodExpressionTest extends Stryker4sSuite {
  describe("ArgMethodExpression") {
    it("should match with a filter call (one argument)") {
      q"list.filter( _ => true )" should matchPattern { case Filter(_, _) => }
    }

    it("should match with an infix filter call (one argument)") {
      q"list filter( _ => true )" should matchPattern { case Filter(_, _) => }
    }

    it("should match with a filter call (block with one argument)") {
      q"list.filter { _ => true }" should matchPattern { case Filter(_, _) => }
    }

    it("should match with an infix filter call (block with one argument)") {
      q"list filter { _ => true }" should matchPattern { case Filter(_, _) => }
    }

    it("should match with a filter call (partial function)") {
      q"list.filter { case Foo(a, b) => true }" should matchPattern { case Filter(_, _) => }
    }

    it("should match with an infix filter call (partial function)") {
      q"list filter { case Foo(a, b) => true }" should matchPattern { case Filter(_, _) => }
    }

    it("should match with a filter call (aux function)") {
      q"list.filter(isBig)" should matchPattern { case Filter(_, _) => }
    }

    it("should match with an infix filter call (aux function)") {
      q"list filter(isBig)" should matchPattern { case Filter(_, _) => }
    }

    it("should not match with an infix filter call (non arguments)") {
      q"list filter()" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with a filter call (more than one argument)") {
      q"list.filter((foo, bar) => true)" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with an infix filter call (more than one argument)") {
      q"list filter((foo, bar) => true)" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with a filter call (more than one argument) 2") {
      q"list.filter(foo, bar)" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with an infix filter call (more than one argument) 2") {
      q"list filter(foo, bar)" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with a filter call (block more than one argument)") {
      q"list.filter { (foo, bar) => true }" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with a filter property") {
      q"foo.filter" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with an infix filter property") {
      q"foo filter" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with a filter import") {
      q"import foo.bar.filter.baz" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with a filter variable") {
      q"filter.foo" should not matchPattern { case Filter(_, _) => }
    }

    it("should match exists to Exists") {
      q"list.exists(_ == 1)" should matchPattern { case Exists(_, _) => }
    }

    it("should match forall to Forall") {
      q"list.forall(_ == 1)" should matchPattern { case Forall(_, _) => }
    }

    it("should match take to Take") {
      q"list.take(1)" should matchPattern { case Take(_, _) => }
    }

    it("should match drop to Drop") {
      q"list.drop(1)" should matchPattern { case Drop(_, _) => }
    }

    it("should match takeRight to TakeRight") {
      q"list.takeRight(1)" should matchPattern { case TakeRight(_, _) => }
    }

    it("should match dropRight to DropRight") {
      q"list.dropRight(1)" should matchPattern { case DropRight(_, _) => }
    }

    it("should match takeWhile to TakeWhile") {
      q"list.takeWhile(_ < 2)" should matchPattern { case TakeWhile(_, _) => }
    }

    it("should match dropWhile to DropWhile") {
      q"list.dropWhile(_ < 2)" should matchPattern { case DropWhile(_, _) => }
    }
  }

  describe("NoArgMethodExpression") {
    it("should not match with an isEmpty call (one argument)") {
      q"list.isEmpty( arg )" should not matchPattern { case IsEmpty(_, _) => }
    }

    it("should not match with an isEmpty call (block with one argument)") {
      q"list.isEmpty { arg }" should not matchPattern { case IsEmpty(_, _) => }
    }

    it("should not match with an isEmpty call (partial function)") {
      q"list.isEmpty { case Foo(a, b) => true }" should not matchPattern { case IsEmpty(_, _) => }
    }

    it("should not match with an isEmpty call (aux function)") {
      q"list.isEmpty(isBig)" should not matchPattern { case IsEmpty(_, _) => }
    }

    it("should not match with an isEmpty call (non arguments)") {
      q"list.isEmpty()" should not matchPattern { case IsEmpty(_, _) => }
    }

    it("should not match with an isEmpty call (more than one argument)") {
      q"list.isEmpty((foo, bar) => true)" should not matchPattern { case IsEmpty(_, _) => }
    }

    it("should not match with an isEmpty call (block more than one argument)") {
      q"list.isEmpty { (foo, bar) => true }" should not matchPattern { case IsEmpty(_, _) => }
    }

    it("should match with an isEmpty property") {
      q"foo.isEmpty" should matchPattern { case IsEmpty(_, _) => }
    }

    it("should not match with an isEmpty import") {
      q"import foo.bar.isEmpty.baz" should not matchPattern { case IsEmpty(_, _) => }
    }

    it("should not match with an isEmpty variable") {
      q"isEmpty.foo" should not matchPattern { case IsEmpty(_, _) => }
    }
  }
}
