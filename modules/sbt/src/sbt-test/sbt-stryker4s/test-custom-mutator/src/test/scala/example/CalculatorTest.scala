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

  test("rejectOversizedOrder fails for quantities above the limit") {
    Calculator.rejectOversizedOrder(101).attempt.map(result => assert(result.isLeft))
  }

  test("rejectOversizedOrder accepts quantities at the limit") {
    Calculator.rejectOversizedOrder(100)
  }

  test("requirePositive accepts positive values") {
    Calculator.requirePositive(1).map(result => assertEquals(result, 1))
  }

  test("requirePositive rejects non-positive values") {
    Calculator.requirePositive(0).attempt.map(result => assert(result.isLeft))
  }

  test("orderTotalWithFallback uses the primary result when it is available") {
    Calculator.orderTotalWithFallback(primaryAvailable = true).map(result => assertEquals(result, 7))
  }

  test("orderTotalWithFallback uses the fallback result when the primary fails") {
    Calculator.orderTotalWithFallback(primaryAvailable = false).map(result => assertEquals(result, 42))
  }

  test("auditedOrderTotal runs the audit effect before computing the result") {
    Calculator.auditedOrderTotal.map(result => assertEquals(result, 11))
  }
}
