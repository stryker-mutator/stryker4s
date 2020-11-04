package stryker4s.testutil

import scala.concurrent.ExecutionContext

import cats.effect.{ContextShift, IO}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{LoneElement, OptionValues}
import stryker4s.log.Logger

abstract class Stryker4sSuite extends AnyFunSpec with Matchers with OptionValues with LoneElement {
  implicit lazy val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  implicit val logger: Logger = new NoopLogger()
}
