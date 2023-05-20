package stryker4s.extension

import cats.kernel.Monoid
import cats.syntax.monoid.*
import stryker4s.testutil.Stryker4sSuite

import scala.meta.Lit

import PartialFunctionOps.*

class PartialFunctionOpsTest extends Stryker4sSuite {

  type TestPF = PartialFunction[Int, List[String]]
  describe("Monoid") {
    val result1 = List("foo", "bar")
    val result2 = List("baz", "qux")

    it("when combined still calls the first PF") {
      val pf1: TestPF = { case 1 => result1 }
      val pf2: TestPF = { case 2 => result2 }

      pf1.combine(pf2)(1) shouldBe result1
    }

    it("when combined still calls the second PF") {
      val pf1: TestPF = { case 1 => result1 }
      val pf2: TestPF = { case 2 => result2 }

      pf1.combine(pf2)(2) shouldBe result2
    }

    it("combines the result when both PFs match") {
      val pf1: TestPF = { case 1 => result1 }
      val pf2: TestPF = { case 1 => result2 }

      pf1.combine(pf2)(1) shouldBe result1 ++ result2
    }

    it("combines the result multiple times with combineN") {
      val pf1: TestPF = { case 1 => result1 }

      pf1.combineN(3)(1) shouldBe result1 ++ result1 ++ result1
    }

    it("Empty Monoid is equal to empty PartialFunction") {
      PartialFunction.empty[Int, List[String]] shouldBe Monoid[TestPF].empty
    }

    it("Empty Monoid should not match on anything equal to empty PartialFunction") {
      Monoid[TestPF].empty.isDefinedAt(0) shouldBe false
    }

    it("should not be able to combine PF's that can't combine the result") {
      "Monoid[PartialFunction[Int, Lit.String]]" shouldNot compile
    }
  }
}
