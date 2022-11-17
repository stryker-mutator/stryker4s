package stryker4jvm.testutil

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.funspec.{AnyFunSpec, AsyncFunSpec}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{LoneElement, OptionValues, Suite}

private[testutil] trait Stryker4jvmBaseSuite extends Matchers with OptionValues with LoneElement { this: Suite => }

abstract class Stryker4jvmSuite extends AnyFunSpec with Stryker4jvmBaseSuite

/** Base suite making it easier to test IO-based code.
  *
  * Every test is forced to return a `IO[scalatest.Assertion]`
  */
abstract class Stryker4jvmIOSuite extends AsyncFunSpec with Stryker4jvmBaseSuite with AsyncIOSpec
