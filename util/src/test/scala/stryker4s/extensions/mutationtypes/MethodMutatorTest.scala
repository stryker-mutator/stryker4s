package stryker4s.extensions.mutationtypes

import stryker4s.Stryker4sSuite
import stryker4s.scalatest.TreeEquality

import scala.meta._

class MethodMutatorTest extends Stryker4sSuite with TreeEquality {

  describe("Filter") {

    it("should match with a filter call (one argument)") {
      q"list.filter( _ => true )" should matchPattern { case Filter(_, _) => }
    }

    it("should match with a filter call (block with one argument)") {
      q"list.filter { _ => true }" should matchPattern { case Filter(_, _) => }
    }

    it("should match with a filter call (partial function)") {
      q"list.filter { case Foo(a, b) => true }" should matchPattern { case Filter(_, _) => }
    }

    it("should match with a filter call (aux function)") {
      q"list.filter(isBig)" should matchPattern { case Filter(_, _) => }
    }

    it("should not match with a filter call (non arguments)") {
      q"list.filter()" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with a filter call (more than one argument)") {
      q"list.filter((foo, bar) => true)" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with a filter call (block more than one argument)") {
      q"list.filter { (foo, bar) => true }" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with a filter property") {
      q"foo.filter" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with a filter import") {
      q"import foo.bar.filter.baz" should not matchPattern { case Filter(_, _) => }
    }

    it("should not match with a filter variable") {
      q"filter.foo" should not matchPattern { case Filter(_, _) => }
    }

  }

  describe("FilterNot") {

    it("should match with a filterNot call (one argument)") {
      q"list.filterNot( _ => true )" should matchPattern { case FilterNot(_, _) => }
    }

    it("should match with a filterNot call (block with one argument)") {
      q"list.filterNot { _ => true }" should matchPattern { case FilterNot(_, _) => }
    }

    it("should match with a filterNot call (partial function)") {
      q"list.filterNot { case Foo(a, b) => true }" should matchPattern { case FilterNot(_, _) => }
    }

    it("should match with a filterNot call (aux function)") {
      q"list.filterNot(isBig)" should matchPattern { case FilterNot(_, _) => }
    }

    it("should not match with a filterNot call (non arguments)") {
      q"list.filterNot()" should not matchPattern { case FilterNot(_, _) => }
    }

    it("should not match with a filterNot call (more than one argument)") {
      q"list.filterNot((foo, bar) => true)" should not matchPattern { case FilterNot(_, _) => }
    }

    it("should not match with a filterNot call (block more than one argument)") {
      q"list.filterNot { (foo, bar) => true }" should not matchPattern { case FilterNot(_, _) => }
    }

    it("should not match with a filterNot property") {
      q"foo.filterNot" should not matchPattern { case FilterNot(_, _) => }
    }

    it("should not match with a filterNot import") {
      q"import foo.bar.filterNot.baz" should not matchPattern { case FilterNot(_, _) => }
    }

    it("should not match with a filterNot variable") {
      q"filterNot.foo" should not matchPattern { case FilterNot(_, _) => }
    }

  }

  describe("Exists") {

    it("should match with an exists call (one argument)") {
      q"list.exists( _ => true )" should matchPattern { case Exists(_, _) => }
    }

    it("should match with an exists call (block with one argument)") {
      q"list.exists { _ => true }" should matchPattern { case Exists(_, _) => }
    }

    it("should match with an exists call (partial function)") {
      q"list.exists { case Foo(a, b) => true }" should matchPattern { case Exists(_, _) => }
    }

    it("should match with an exists call (aux function)") {
      q"list.exists(isBig)" should matchPattern { case Exists(_, _) => }
    }

    it("should not match with an exists call (non arguments)") {
      q"list.exists()" should not matchPattern { case Exists(_, _) => }
    }

    it("should not match with an exists call (more than one argument)") {
      q"list.exists((foo, bar) => true)" should not matchPattern { case Exists(_, _) => }
    }

    it("should not match with an exists call (block more than one argument)") {
      q"list.exists { (foo, bar) => true }" should not matchPattern { case Exists(_, _) => }
    }

    it("should not match with an exists property") {
      q"foo.exists" should not matchPattern { case Exists(_, _) => }
    }

    it("should not match with an exists import") {
      q"import foo.bar.exists.baz" should not matchPattern { case Exists(_, _) => }
    }

    it("should not match with an exists variable") {
      q"exists.foo" should not matchPattern { case Exists(_, _) => }
    }

  }

  describe("ForAll") {

    it("should match with a forAll call (one argument)") {
      q"list.forAll( _ => true )" should matchPattern { case ForAll(_, _) => }
    }

    it("should match with a forAll call (block with one argument)") {
      q"list.forAll { _ => true }" should matchPattern { case ForAll(_, _) => }
    }

    it("should match with a forAll call (partial function)") {
      q"list.forAll { case Foo(a, b) => true }" should matchPattern { case ForAll(_, _) => }
    }

    it("should match with a forAll call (aux function)") {
      q"list.forAll(isBig)" should matchPattern { case ForAll(_, _) => }
    }

    it("should not match with a forAll call (non arguments)") {
      q"list.forAll()" should not matchPattern { case ForAll(_, _) => }
    }

    it("should not match with a forAll call (more than one argument)") {
      q"list.forAll((foo, bar) => true)" should not matchPattern { case ForAll(_, _) => }
    }

    it("should not match with a forAll call (block more than one argument)") {
      q"list.forAll { (foo, bar) => true }" should not matchPattern { case ForAll(_, _) => }
    }

    it("should not match with a forAll property") {
      q"foo.forAll" should not matchPattern { case ForAll(_, _) => }
    }

    it("should not match with a forAll import") {
      q"import foo.bar.forAll.baz" should not matchPattern { case ForAll(_, _) => }
    }

    it("should not match with a forAll variable") {
      q"forAll.foo" should not matchPattern { case ForAll(_, _) => }
    }

  }

  describe("IsEmpty") {

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

  describe("NonEmpty") {

    it("should not match with a nonEmpty call (one argument)") {
      q"list.nonEmpty( arg )" should not matchPattern { case NonEmpty(_, _) => }
    }

    it("should not match with a nonEmpty call (block with one argument)") {
      q"list.nonEmpty { arg }" should not matchPattern { case NonEmpty(_, _) => }
    }

    it("should not match with a nonEmpty call (partial function)") {
      q"list.nonEmpty { case Foo(a, b) => true }" should not matchPattern { case NonEmpty(_, _) => }
    }

    it("should not match with a nonEmpty call (aux function)") {
      q"list.nonEmpty(isBig)" should not matchPattern { case NonEmpty(_, _) => }
    }

    it("should not match with a nonEmpty call (non arguments)") {
      q"list.nonEmpty()" should not matchPattern { case NonEmpty(_, _) => }
    }

    it("should not match with a nonEmpty call (more than one argument)") {
      q"list.nonEmpty((foo, bar) => true)" should not matchPattern { case NonEmpty(_, _) => }
    }

    it("should not match with a nonEmpty call (block more than one argument)") {
      q"list.nonEmpty { (foo, bar) => true }" should not matchPattern { case NonEmpty(_, _) => }
    }

    it("should match with a nonEmpty property") {
      q"foo.nonEmpty" should matchPattern { case NonEmpty(_, _) => }
    }

    it("should not match with a nonEmpty import") {
      q"import foo.bar.nonEmpty.baz" should not matchPattern { case NonEmpty(_, _) => }
    }

    it("should not match with a nonEmpty variable") {
      q"nonEmpty.foo" should not matchPattern { case NonEmpty(_, _) => }
    }

  }

  describe("IndexOf") {

    it("should match with an indexOf call (one argument)") {
      q"list.indexOf( _ => true )" should matchPattern { case IndexOf(_, _) => }
    }

    it("should match with an indexOf call (block with one argument)") {
      q"list.indexOf { _ => true }" should matchPattern { case IndexOf(_, _) => }
    }

    it("should match with an indexOf call (partial function)") {
      q"list.indexOf { case Foo(a, b) => true }" should matchPattern { case IndexOf(_, _) => }
    }

    it("should match with an indexOf call (aux function)") {
      q"list.indexOf(isBig)" should matchPattern { case IndexOf(_, _) => }
    }

    it("should not match with an indexOf call (non arguments)") {
      q"list.indexOf()" should not matchPattern { case IndexOf(_, _) => }
    }

    it("should not match with an indexOf call (more than one argument)") {
      q"list.indexOf((foo, bar) => true)" should not matchPattern { case IndexOf(_, _) => }
    }

    it("should not match with an indexOf call (block more than one argument)") {
      q"list.indexOf { (foo, bar) => true }" should not matchPattern { case IndexOf(_, _) => }
    }

    it("should not match with an indexOf property") {
      q"foo.indexOf" should not matchPattern { case IndexOf(_, _) => }
    }

    it("should not match with an indexOf import") {
      q"import foo.bar.indexOf.baz" should not matchPattern { case IndexOf(_, _) => }
    }

    it("should not match with an indexOf variable") {
      q"indexOf.foo" should not matchPattern { case IndexOf(_, _) => }
    }

  }

  describe("LastIndexOf") {

    it("should match with a lastIndexOf call (one argument)") {
      q"list.lastIndexOf( _ => true )" should matchPattern { case LastIndexOf(_, _) => }
    }

    it("should match with a lastIndexOf call (block with one argument)") {
      q"list.lastIndexOf { _ => true }" should matchPattern { case LastIndexOf(_, _) => }
    }

    it("should match with a lastIndexOf call (partial function)") {
      q"list.lastIndexOf { case Foo(a, b) => true }" should matchPattern { case LastIndexOf(_, _) => }
    }

    it("should match with a lastIndexOf call (aux function)") {
      q"list.lastIndexOf(isBig)" should matchPattern { case LastIndexOf(_, _) => }
    }

    it("should not match with a lastIndexOf call (non arguments)") {
      q"list.lastIndexOf()" should not matchPattern { case LastIndexOf(_, _) => }
    }

    it("should not match with a lastIndexOf call (more than one argument)") {
      q"list.lastIndexOf((foo, bar) => true)" should not matchPattern { case LastIndexOf(_, _) => }
    }

    it("should not match with a lastIndexOf call (block more than one argument)") {
      q"list.lastIndexOf { (foo, bar) => true }" should not matchPattern { case LastIndexOf(_, _) => }
    }

    it("should not match with a lastIndexOf property") {
      q"foo.lastIndexOf" should not matchPattern { case LastIndexOf(_, _) => }
    }

    it("should not match with a lastIndexOf import") {
      q"import foo.bar.lastIndexOf.baz" should not matchPattern { case LastIndexOf(_, _) => }
    }

    it("should not match with a lastIndexOf variable") {
      q"lastIndexOf.foo" should not matchPattern { case LastIndexOf(_, _) => }
    }

  }

  describe("Max") {

    it("should not match with a max call (one argument)") {
      q"list.max( arg )" should not matchPattern { case Max(_, _) => }
    }

    it("should not match with a max call (block with one argument)") {
      q"list.max { arg }" should not matchPattern { case Max(_, _) => }
    }

    it("should not match with a max call (partial function)") {
      q"list.max { case Foo(a, b) => true }" should not matchPattern { case Max(_, _) => }
    }

    it("should not match with a max call (aux function)") {
      q"list.max(isBig)" should not matchPattern { case Max(_, _) => }
    }

    it("should not match with a max call (non arguments)") {
      q"list.max()" should not matchPattern { case Max(_, _) => }
    }

    it("should not match with a max call (more than one argument)") {
      q"list.max((foo, bar) => true)" should not matchPattern { case Max(_, _) => }
    }

    it("should not match with a max call (block more than one argument)") {
      q"list.max { (foo, bar) => true }" should not matchPattern { case Max(_, _) => }
    }

    it("should match with a max property") {
      q"foo.max" should matchPattern { case Max(_, _) => }
    }

    it("should not match with a max import") {
      q"import foo.bar.max.baz" should not matchPattern { case Max(_, _) => }
    }

    it("should not match with a max variable") {
      q"max.foo" should not matchPattern { case Max(_, _) => }
    }

  }

  describe("Min") {

    it("should not match with a min call (one argument)") {
      q"list.min( arg )" should not matchPattern { case Min(_, _) => }
    }

    it("should not match with a min call (block with one argument)") {
      q"list.min { arg }" should not matchPattern { case Min(_, _) => }
    }

    it("should not match with a min call (partial function)") {
      q"list.min { case Foo(a, b) => true }" should not matchPattern { case Min(_, _) => }
    }

    it("should not match with a min call (aux function)") {
      q"list.min(isBig)" should not matchPattern { case Min(_, _) => }
    }

    it("should not match with a min call (non arguments)") {
      q"list.min()" should not matchPattern { case Min(_, _) => }
    }

    it("should not match with a min call (more than one argument)") {
      q"list.min((foo, bar) => true)" should not matchPattern { case Min(_, _) => }
    }

    it("should not match with a min call (block more than one argument)") {
      q"list.min { (foo, bar) => true }" should not matchPattern { case Min(_, _) => }
    }

    it("should match with a min property") {
      q"foo.min" should matchPattern { case Min(_, _) => }
    }

    it("should not match with a min import") {
      q"import foo.bar.min.baz" should not matchPattern { case Min(_, _) => }
    }

    it("should not match with a min variable") {
      q"min.foo" should not matchPattern { case Min(_, _) => }
    }

  }

  describe("MaxBy") {

    it("should match with a maxBy call (one argument)") {
      q"list.maxBy( _ => true )" should matchPattern { case MaxBy(_, _) => }
    }

    it("should match with a maxBy call (block with one argument)") {
      q"list.maxBy { _ => true }" should matchPattern { case MaxBy(_, _) => }
    }

    it("should match with a maxBy call (partial function)") {
      q"list.maxBy { case Foo(a, b) => true }" should matchPattern { case MaxBy(_, _) => }
    }

    it("should match with a maxBy call (aux function)") {
      q"list.maxBy(isBig)" should matchPattern { case MaxBy(_, _) => }
    }

    it("should not match with a maxBy call (non arguments)") {
      q"list.maxBy()" should not matchPattern { case MaxBy(_, _) => }
    }

    it("should not match with a maxBy call (more than one argument)") {
      q"list.maxBy((foo, bar) => true)" should not matchPattern { case MaxBy(_, _) => }
    }

    it("should not match with a maxBy call (block more than one argument)") {
      q"list.maxBy { (foo, bar) => true }" should not matchPattern { case MaxBy(_, _) => }
    }

    it("should not match with a maxBy property") {
      q"foo.maxBy" should not matchPattern { case MaxBy(_, _) => }
    }

    it("should not match with a maxBy import") {
      q"import foo.bar.maxBy.baz" should not matchPattern { case MaxBy(_, _) => }
    }

    it("should not match with a maxBy variable") {
      q"maxBy.foo" should not matchPattern { case MaxBy(_, _) => }
    }

  }

  describe("MinBy") {

    it("should match with a minBy call (one argument)") {
      q"list.minBy( _ => true )" should matchPattern { case MinBy(_, _) => }
    }

    it("should match with a minBy call (block with one argument)") {
      q"list.minBy { _ => true }" should matchPattern { case MinBy(_, _) => }
    }

    it("should match with a minBy call (partial function)") {
      q"list.minBy { case Foo(a, b) => true }" should matchPattern { case MinBy(_, _) => }
    }

    it("should match with a minBy call (aux function)") {
      q"list.minBy(isBig)" should matchPattern { case MinBy(_, _) => }
    }

    it("should not match with a minBy call (non arguments)") {
      q"list.minBy()" should not matchPattern { case MinBy(_, _) => }
    }

    it("should not match with a minBy call (more than one argument)") {
      q"list.minBy((foo, bar) => true)" should not matchPattern { case MinBy(_, _) => }
    }

    it("should not match with a minBy call (block more than one argument)") {
      q"list.minBy { (foo, bar) => true }" should not matchPattern { case MinBy(_, _) => }
    }

    it("should not match with a minBy property") {
      q"foo.minBy" should not matchPattern { case MinBy(_, _) => }
    }

    it("should not match with a minBy import") {
      q"import foo.bar.minBy.baz" should not matchPattern { case MinBy(_, _) => }
    }

    it("should not match with a minBy variable") {
      q"minBy.foo" should not matchPattern { case MinBy(_, _) => }
    }

  }
  
}
