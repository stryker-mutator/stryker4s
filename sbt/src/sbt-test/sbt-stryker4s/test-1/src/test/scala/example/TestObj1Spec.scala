package example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Spec extends AnyFlatSpec with Matchers {
  it should "check for a's in mutatesOkay" in {
    TestObj.mutatesOkay(" a ") shouldBe true
    TestObj.mutatesOkay(" b ") shouldBe false
  }

  it should "return false if a file does not exists" in {
    TestObj.test2("/home/blah/fake") shouldBe false
  }

  it should "check for b's in alsoMutatesOkay" in {
    TestObj.alsoMutatesOkay(" b ") shouldBe true
    TestObj.alsoMutatesOkay(" a ") shouldBe false
  }

}
