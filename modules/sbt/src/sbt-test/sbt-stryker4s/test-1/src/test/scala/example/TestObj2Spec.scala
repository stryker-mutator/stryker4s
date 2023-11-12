package example

class Spec2 extends munit.FunSuite {
  test("check the string") {
    assert(!TestObj2.str("hi"))
    assert(TestObj2.str("blah"))
  }
}
