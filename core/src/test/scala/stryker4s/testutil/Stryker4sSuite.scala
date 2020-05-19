package stryker4s.testutil

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.{AnyFunSpec, AsyncFunSpec}
import org.scalatest.{LoneElement, OptionValues}
import org.scalatest.Suite

trait Stryker4sSuite {
  this: Suite =>
}

abstract class SyncStryker4sSuite
    extends AnyFunSpec
    with Matchers
    with OptionValues
    with LoneElement
    with Stryker4sSuite

abstract class AsyncStryker4sSuite
    extends AsyncFunSpec
    with Matchers
    with OptionValues
    with LoneElement
    with Stryker4sSuite
