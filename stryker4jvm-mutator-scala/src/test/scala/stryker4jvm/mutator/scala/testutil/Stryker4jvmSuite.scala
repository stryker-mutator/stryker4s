package stryker4jvm.mutator.scala.testutil

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{LoneElement, OptionValues, Suite}

private[testutil] trait Stryker4jvmBaseSuite extends Matchers with OptionValues with LoneElement { this: Suite => }

abstract class Stryker4jvmSuite extends AnyFunSpec with Stryker4jvmBaseSuite
