package stryker4s.testutil

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.{LoneElement, OptionValues}
import cats.effect.ContextShift
import cats.effect.IO
import scala.concurrent.ExecutionContext
abstract class Stryker4sSuite extends AnyFunSpec with Matchers with OptionValues with LoneElement {
  implicit lazy val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
}
