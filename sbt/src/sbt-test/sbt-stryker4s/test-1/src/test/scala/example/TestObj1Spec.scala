package example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Spec extends AnyFlatSpec with Matchers {
  it should "return the lesser" in {
    TestObj.least(1, 2) shouldBe 1
    TestObj.least(2, 1) shouldBe 1
  }

  it should "return 0 if equal" in {
    TestObj.least(0, 0) shouldBe 0
  }

  it should "return false if a file does not exists" in {
    TestObj.test2("/home/blah/fake") shouldBe false
  }
}