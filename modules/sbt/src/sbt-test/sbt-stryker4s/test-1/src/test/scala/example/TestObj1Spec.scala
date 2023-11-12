package example

class Spec extends munit.FunSuite {
  test("check for a's in mutatesOkay") {
    assert(TestObj.mutatesOkay(" a "))
    assert(!TestObj.mutatesOkay(" b "))
  }

  test("return false if a file does not exists") {
    assert(!TestObj.test2("/home/blah/fake"))
  }

  test("check for b's in alsoMutatesOkay") {
    assert(TestObj.alsoMutatesOkay(" b "))
    assert(!TestObj.alsoMutatesOkay(" a "))
  }

}
