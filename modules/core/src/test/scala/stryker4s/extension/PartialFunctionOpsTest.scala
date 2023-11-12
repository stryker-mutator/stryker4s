package stryker4s.extension

import cats.kernel.Monoid
import cats.syntax.monoid.*
import stryker4s.testkit.Stryker4sSuite

import scala.meta.Lit

import PartialFunctionOps.*

class PartialFunctionOpsTest extends Stryker4sSuite {

  type TestPF = PartialFunction[Int, List[String]]
  describe("Monoid") {
    val result1 = List("foo", "bar")
    val result2 = List("baz", "qux")

    test("when combined still calls the first PF") {
      val pf1: TestPF = { case 1 => result1 }
      val pf2: TestPF = { case 2 => result2 }

      assertEquals(pf1.combine(pf2)(1), result1)
    }

    test("when combined still calls the second PF") {
      val pf1: TestPF = { case 1 => result1 }
      val pf2: TestPF = { case 2 => result2 }

      assertEquals(pf1.combine(pf2)(2), result2)
    }

    test("combines the result when both PFs match") {
      val pf1: TestPF = { case 1 => result1 }
      val pf2: TestPF = { case 1 => result2 }

      assertEquals(pf1.combine(pf2)(1), result1 ++ result2)
    }

    test("combines the result multiple times with combineN") {
      val pf1: TestPF = { case 1 => result1 }

      assertEquals(pf1.combineN(3)(1), result1 ++ result1 ++ result1)
    }

    test("Empty Monoid is equal to empty PartialFunction") {
      assertEquals(PartialFunction.empty[Int, List[String]], Monoid[TestPF].empty)
    }

    test("Empty Monoid should not match on anything equal to empty PartialFunction") {
      assert(!Monoid[TestPF].empty.isDefinedAt(0))
    }

    test("should not be able to combine PF's that can't combine the result") {
      compileErrors("Monoid[PartialFunction[Int, Lit.String]]")
    }
  }
}
