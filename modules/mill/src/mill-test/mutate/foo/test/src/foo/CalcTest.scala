package foo

class CalcTest extends munit.FunSuite {
  test("add") {
    assertEquals(Calc.add(2, 3), 5)
  }

  // `isPositive` is covered but only weakly asserted, so some of its mutants are killed while
  // others survive. This gives a deterministic, partial mutation score for the test to assert on.
  test("isPositive") {
    assert(Calc.isPositive(1))
    assert(!Calc.isPositive(-1))
  }
}
