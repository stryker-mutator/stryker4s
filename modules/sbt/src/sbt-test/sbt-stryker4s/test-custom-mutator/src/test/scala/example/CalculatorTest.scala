package example

class CalculatorTest extends munit.CatsEffectSuite {
  test("add sums two numbers") {
    assertEquals(Calculator.add(2, 3), 5)
    assertEquals(Calculator.add(-1, 1), 0)
  }

  test("subtract subtracts two numbers") {
    assertEquals(Calculator.subtract(5, 3), 2)
    assertEquals(Calculator.subtract(1, 1), 0)
  }

  test("isLargeOrder detects quantities above the threshold") {
    assert(Calculator.isLargeOrder(11))
    assert(!Calculator.isLargeOrder(10))
    assert(!Calculator.isLargeOrder(0))
  }

  test("defaultDiscounts has exactly the expected three tiers") {
    assertEquals(Calculator.defaultDiscounts, List(5, 10, 15))
  }

  test("safeDivide recovers from division-by-zero errors back to a default of 0") {
    Calculator.safeDivide(10, 0).map(result => assertEquals(result, 0))
  }

  test("safeDivide passes through a successful division") {
    Calculator.safeDivide(10, 2).map(result => assertEquals(result, 5))
  }
}
