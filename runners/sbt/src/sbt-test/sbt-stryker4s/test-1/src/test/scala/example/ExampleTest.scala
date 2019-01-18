package example

import org.scalatest._

class ExampleTest extends FlatSpec with Matchers {
  "Person" should "be able to drink with age 18" in {
    Example.canDrink(18) should be (true)
  }
}