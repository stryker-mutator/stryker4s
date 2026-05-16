package example

class CalculatorTest extends munit.FunSuite {
  test("add sums two numbers") {
    assertEquals(Calculator.add(2, 3), 5)
    assertEquals(Calculator.add(-1, 1), 0)
  }

  test("isPositive distinguishes positive from non-positive") {
    assert(Calculator.isPositive(1))
    assert(!Calculator.isPositive(0))
    assert(!Calculator.isPositive(-1))
  }
}
