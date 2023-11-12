package example

class IgnoreMeTest extends munit.FunSuite {
  test("not be able to drink with age 16") {
    fail("This test should never be run because of the test-filter")
  }

}
