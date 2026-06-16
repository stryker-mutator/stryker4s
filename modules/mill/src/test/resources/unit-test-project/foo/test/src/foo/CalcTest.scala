package foo

class CalcTest extends munit.FunSuite {
  test("add") {
    assertEquals(Calc.add(2, 3), 5)
  }
}
