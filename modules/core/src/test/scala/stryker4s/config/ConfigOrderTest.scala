package stryker4s.config

import cats.data.NonEmptyList
import stryker4s.testkit.Stryker4sSuite

class ConfigOrderTest extends Stryker4sSuite {
  test("should order ascending") {
    val order = NonEmptyList.of(ConfigOrder(1), ConfigOrder(0), ConfigOrder.Last, ConfigOrder(Int.MinValue))

    assertEquals(
      order.sorted,
      NonEmptyList.of(ConfigOrder(Int.MinValue), ConfigOrder(0), ConfigOrder(1), ConfigOrder.Last)
    )
  }
}
