package stryker4s.testutil

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.funspec.{AnyFunSpec, AsyncFunSpec}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{LoneElement, OptionValues, Suite}

private[testutil] trait Stryker4sBaseSuite extends Matchers with OptionValues with LoneElement { this: Suite => }

abstract class Stryker4sSuite extends AnyFunSpec with Stryker4sBaseSuite

/** Base suite making it easier to test IO-based code
  * Every test is forced to return a `IO[scalatest.Assertion]`
  */
abstract class Stryker4sIOSuite extends AsyncFunSpec with Stryker4sBaseSuite with AsyncIOSpec
