package stryker4s.extension.mutationtype

import stryker4s.scalatest.TreeEquality
import stryker4s.testutil.Stryker4sSuite

import scala.meta._

class MethodExpressionTest extends Stryker4sSuite with TreeEquality {

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
