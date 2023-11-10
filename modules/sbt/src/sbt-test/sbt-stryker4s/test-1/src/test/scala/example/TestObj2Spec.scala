package example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Spec2 extends AnyFlatSpec with Matchers {
  it should "check the string" in {
    TestObj2.str("hi") shouldBe false
    TestObj2.str("blah") shouldBe true
  }
}
