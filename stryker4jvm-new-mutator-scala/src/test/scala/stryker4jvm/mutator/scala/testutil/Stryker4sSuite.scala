package stryker4jvm.mutator.scala.testutil

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{LoneElement, OptionValues, Suite}

private[testutil] trait Stryker4sBaseSuite extends Matchers with OptionValues with LoneElement { this: Suite => }

abstract class Stryker4sSuite extends AnyFunSpec with Stryker4sBaseSuite
