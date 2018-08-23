package stryker4s

import org.scalatest.{FunSpec, LoneElement, Matchers, OptionValues}
import stryker4s.scalatest.LogMatchers

trait Stryker4sSuite extends FunSpec with Matchers with OptionValues with LoneElement with LogMatchers
