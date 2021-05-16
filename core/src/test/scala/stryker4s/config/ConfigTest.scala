package stryker4s.config

import stryker4s.testutil.Stryker4sSuite

class ConfigTest extends Stryker4sSuite {
  describe("concurrency") {
    val expectedConcurrencies = Map(
      1 -> 1,
      2 -> 2,
      3 -> 2,
      4 -> 2,
      6 -> 3,
      8 -> 3,
      10 -> 4,
      12 -> 4,
      16 -> 5,
      20 -> 6,
      24 -> 7,
      28 -> 8,
      32 -> 9
    )

    expectedConcurrencies.foreach { case (cpuCoreCount, expectedConcurrency) =>
      it(s"should give concurrency $expectedConcurrency for $cpuCoreCount cpu cores") {
        Config.concurrencyFor(cpuCoreCount) shouldBe expectedConcurrency
      }
    }
  }
}
