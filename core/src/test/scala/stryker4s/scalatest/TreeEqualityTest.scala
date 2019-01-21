package stryker4s.scalatest

import stryker4s.testutil.Stryker4sSuite

import scala.meta._

class TreeEqualityInScopeTest extends Stryker4sSuite with TreeEquality {

  describe("Equality in scope") {
    it("two different tree objects with same structure should equal") {
      val firstTree = q"18 > 5"
      val secondTree = q"18 > 5"

      firstTree should equal(secondTree)
    }

    it("one single tree should equal") {
      val first = q"18 > 5"
      val alsoFirst = first

      first should equal(alsoFirst)
      first should be theSameInstanceAs alsoFirst
    }

    it("two different trees with different structure should not equal") {
      val first = q"18 > 5"
      val second = q"val a = 5"

      first shouldNot equal(second)
      first should not be second
    }

    it("two different trees with postfix and infix operator should not equal") {
      val first = q"val a = b.+(c)"
      val second = q"val a = b + c"
      first shouldNot equal(second)
    }

    it("two trees with same structure but different syntax should not equal") {
      val first = q"def foo = 15 > 14"
      val second = q"def foo = 15 < 14"

      first should not equal second
    }

    it("two sources with different syntax should not equal") {
      val first: Source = "class Foo { def bar: Boolean = 15 > 14 }".parse[Source].get
      val second: Source = "class Foo { def bar: Boolean = 15 < 14 }".parse[Source].get

      first should not equal second
    }

    it("two sources with same syntax should equal") {
      val first: Source = "class Foo { def bar: Boolean = 15 > 14 }".parse[Source].get
      val second: Source = "class Foo { def bar: Boolean = 15 > 14 }".parse[Source].get

      first should equal(second)
    }
  }
}

class TreeEqualityOutOfScopeTest extends Stryker4sSuite {
  describe("Equality out of scope") {
    it("two different trees with same structure should not equal") {
      val firstTree = q"18 > 5"
      val secondTree = q"18 > 5"

      firstTree should not equal secondTree
      firstTree should not be secondTree
    }

    it("one single tree should equal and 'be'") {
      val first = q"18 > 5"
      val alsoFirst = first

      first should equal(alsoFirst)
      first should be theSameInstanceAs alsoFirst
    }
  }
}
