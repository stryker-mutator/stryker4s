package stryker4jvm.extensions

import stryker4jvm.testutil.Stryker4jvmSuite
import stryker4jvm.extensions.NumberExtensions.*

class NumberExtensionsTest extends Stryker4jvmSuite {

  describe("roundDecimals") {
    it("should round down to the given decimals") {
      1.51.roundDecimals(1) shouldBe 1.5
    }

    it("should round up to the given decimals") {
      1.55.roundDecimals(1) shouldBe 1.6
    }

    it("should round multiple decimals") {
      1.55555.roundDecimals(4) shouldBe 1.5556
    }

    it("should leave NaN") {
      Double.NaN.roundDecimals(1).isNaN() shouldBe true
    }
  }
}
