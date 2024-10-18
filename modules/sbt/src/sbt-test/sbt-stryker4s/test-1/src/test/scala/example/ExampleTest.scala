package example

class ExampleTest extends munit.FunSuite {
  test("be able to drink with age 18") {
    assert(Example.canDrink(18))
  }

}
