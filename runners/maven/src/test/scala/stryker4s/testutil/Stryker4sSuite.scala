package stryker4s.testutil

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.{LoneElement, OptionValues}

 abstract class Stryker4sSuite extends AnyFunSpec with Matchers with OptionValues with LoneElement
