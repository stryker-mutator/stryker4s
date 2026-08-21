package example

class CalculatorTest extends munit.FunSuite {
  test("add sums two numbers") {
    assertEquals(Calculator.add(2, 3), 5)
    assertEquals(Calculator.add(-1, 1), 0)
  }

  test("subtract subtracts two numbers") {
    assertEquals(Calculator.subtract(5, 3), 2)
    assertEquals(Calculator.subtract(1, 1), 0)
  }
}
