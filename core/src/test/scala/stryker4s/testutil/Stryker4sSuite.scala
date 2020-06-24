package stryker4s.testutil

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.{AnyFunSpec, AsyncFunSpec}
import org.scalatest.{LoneElement, OptionValues}

abstract class Stryker4sSuite extends AnyFunSpec with Matchers with OptionValues with LoneElement

abstract class AsyncStryker4sSuite extends AsyncFunSpec with Matchers with OptionValues with LoneElement
