package stryker4s.testutil

import scala.concurrent.ExecutionContext

import cats.effect.IO
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{LoneElement, OptionValues}
import stryker4s.log.Logger

abstract class Stryker4sSuite extends AnyFunSpec with Matchers with OptionValues with LoneElement {
  implicit val logger: Logger = new NoopLogger()
}
