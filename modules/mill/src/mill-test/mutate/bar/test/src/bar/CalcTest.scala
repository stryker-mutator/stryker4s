package bar

class CalcTest extends munit.FunSuite {
  test("add") {
    assertEquals(Calc.add(2, 3), 5)
    assertEquals(Calc.add(-1, 1), 0)
  }

  test("subtract") {
    assertEquals(Calc.subtract(5, 3), 2)
    assertEquals(Calc.subtract(1, 1), 0)
  }
}
