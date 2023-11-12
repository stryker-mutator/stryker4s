package stryker4s.extension

import stryker4s.extension.NumberExtensions.*
import stryker4s.testkit.Stryker4sSuite

class NumberExtensionsTest extends Stryker4sSuite {

  describe("roundDecimals") {
    test("should round down to the given decimals") {
      assertEquals(1.51.roundDecimals(1), 1.5)
    }

    test("should round up to the given decimals") {
      assertEquals(1.55.roundDecimals(1), 1.6)
    }

    test("should round multiple decimals") {
      assertEquals(1.55555.roundDecimals(4), 1.5556)
    }

    test("should leave NaN") {
      assert(Double.NaN.roundDecimals(1).isNaN())
    }
  }
}
