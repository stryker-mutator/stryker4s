package stryker4s

import org.scalatest.{FunSpec, LoneElement, Matchers, OptionValues}
import stryker4s.scalatest.LogMatchers

trait Stryker4sSuite extends FunSpec with Matchers with OptionValues with LoneElement with LogMatchers {

  /**
    * The className of the system under test.
    * Is done by getting the executing test class and stripping off 'test'.
    *
    * This is used for logMatchers.
    */
  implicit val className: String =  getClass.getCanonicalName.replace("Test", "")
}
