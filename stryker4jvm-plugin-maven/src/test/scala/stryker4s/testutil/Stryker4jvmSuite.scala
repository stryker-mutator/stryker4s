package stryker4s.testutil

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{LoneElement, OptionValues}
import stryker4jvm.core.logging.Logger

abstract class Stryker4jvmSuite extends AnyFunSpec with Matchers with OptionValues with LoneElement {
  implicit val logger: Logger = new NoopLogger()
}
