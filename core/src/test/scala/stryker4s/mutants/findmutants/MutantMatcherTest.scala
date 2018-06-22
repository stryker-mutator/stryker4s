package stryker4s.mutants.findmutants

import stryker4s.Stryker4sSuite
import stryker4s.extensions.ImplicitMutationConversion.mutationToTree
import stryker4s.extensions.mutationtypes._
import stryker4s.model.FoundMutant
import stryker4s.scalatest.TreeEquality

import scala.meta._

class MutantMatcherTest extends Stryker4sSuite with TreeEquality {
  val sut = new MutantMatcher

  def checkMatch(matchFun: PartialFunction[Tree, FoundMutant],
                 tree: Tree,
                 original: Term,
                 matches: Term*): Unit = {
    val found = tree collect matchFun

    val result = found.loneElement
    result.originalTree should equal(original)
    result.mutations should contain theSameElementsAs matches
  }

  describe("All Matchers") {
    it("should match a conditional statement") {
      val tree = q"def foo = 15 > 20 && 20 < 15"

      val found = tree collect sut.allMatchers()

      found should have length 2
      found.head.originalTree should equal(q">")
      found.head.mutations should contain only (q">=", q"<", q"==")
      found(1).originalTree should equal(q"<")
      found(1).mutations should contain only (q"<=", q">", q"==")
    }

    it("should match a method") {
      val tree = q"def foo = List(1, 2).filterNot(filterNotFunc).filter(filterFunc)"

      val found = tree collect sut.allMatchers()

      found should have length 2
      found.head.originalTree should equal(q"filterNot")
      found.head.mutations should contain only q"filter"
      found(1).originalTree should equal(q"filter")
      found(1).mutations should contain only q"filterNot"
    }

    it("should match a boolean and a conditional") {
      val tree = q"def foo = false && 15 > 4"

      val found = tree collect sut.allMatchers()

      found should have length 2
      found.head.originalTree should equal(q"false")
      found.head.mutations should contain only q"true"
      found(1).originalTree should equal(q">")
      found(1).mutations should contain allOf (q">=", q"<", q"==")
    }

    it("should match the default case of a constructor argument") {
      val tree = q"class Person(isOld: Boolean = 18 > 15) { }"

      val found = tree collect sut.allMatchers()

      val head = found.loneElement
      head.originalTree should equal(q">")
      head.mutations should contain allOf (q">=", q"<", q"==")
    }

    it("should match on the default case of a function argument") {
      val tree = q"def hasGoodBack(isOld: Boolean = age > 60): Boolean = isOld"

      val found = tree collect sut.allMatchers()

      val head = found.loneElement
      head.originalTree should equal(q">")
      head.mutations should contain allOf (q">=", q"<", q"==")
    }
  }

  describe("matchConditionalStatements matcher") {
    it("should match >= sign with >, <, and ==") {
      checkMatch(
        sut.matchConditionals(),
        q"def foo = 18 >= 20",
        GreaterThanEqualTo,
        GreaterThan,
        LesserThan,
        EqualTo
      )
    }

    it("should match > with >=, < and ==") {
      checkMatch(
        sut.matchConditionals(),
        q"def foo = 18 > 20",
        GreaterThan,
        GreaterThanEqualTo,
        LesserThan,
        EqualTo
      )
    }

    it("should match <= to <, >= and ==") {
      checkMatch(
        sut.matchConditionals(),
        q"def foo = 18 <= 20",
        LesserThanEqualTo,
        LesserThan,
        GreaterThanEqualTo,
        EqualTo
      )
    }

    it("should match < to <=, > and ==") {
      checkMatch(
        sut.matchConditionals(),
        q"def foo = 18 < 20",
        LesserThan,
        LesserThanEqualTo,
        GreaterThan,
        EqualTo
      )
    }

    it("should match == to !=") {
      checkMatch(
        sut.matchConditionals(),
        q"def foo = 18 == 20",
        EqualTo,
        NotEqualTo
      )
    }

    it("should match != to ==") {
      checkMatch(
        sut.matchConditionals(),
        q"def foo = 18 != 20",
        NotEqualTo,
        EqualTo
      )
    }
  }

  describe("matchMethods matcher") {
    it("should match filter to filterNot") {
      checkMatch(
        sut.matchMethods(),
        q"def foo = List(1, 2, 3).filter(_ % 2 == 0)",
        Filter,
        FilterNot
      )
    }

    it("should match filterNot to filter") {
      checkMatch(
        sut.matchMethods(),
        q"def foo = List(1, 2, 3).filterNot(_ % 2 == 0)",
        FilterNot,
        Filter
      )
    }
  }

  describe("booleanSubstitutions matcher") {
    it("should match false to true") {
      checkMatch(
        sut.matchBooleanSubstitutions(),
        q"def foo = false",
        False,
        True
      )
    }

    it("should match true to false") {
      checkMatch(
        sut.matchBooleanSubstitutions(),
        q"def foo = true",
        True,
        False
      )
    }
  }

}
