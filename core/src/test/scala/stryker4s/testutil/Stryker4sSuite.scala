package stryker4s.testutil

import cats.effect.testing.scalatest.{AssertingSyntax, EffectTestSupport}
import cats.effect.{ContextShift, IO, Timer}
import org.scalatest.funspec.{AnyFunSpec, AsyncFunSpec}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, LoneElement, OptionValues, Suite}

private[testutil] trait Stryker4sBaseSuite extends Matchers with OptionValues with LoneElement { this: Suite => }

abstract class Stryker4sSuite extends AnyFunSpec with Stryker4sBaseSuite

/** Base suite making it easier to test IO-based code
  * Every test is forced to return a `IO[scalatest.Assertion]`
  */
abstract class Stryker4sIOSuite extends AsyncFunSpec with Stryker4sBaseSuite with AsyncIOSpec

// https://github.com/djspiewak/cats-effect-testing/blob/master/scalatest/src/main/scala/cats/effect/testing/scalatest/AsyncIOSpec.scala
// Same as cats.effect.testing.scalatest.AsyncIOSpec but without overriding the ExecutionContext
// We want to keep ScalaTests default single-threaded EC to prevent any issues with concurrent tests
trait AsyncIOSpec extends AssertingSyntax with EffectTestSupport { asyncTestSuite: AsyncTestSuite =>
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(executionContext)
  implicit val ioTimer: Timer[IO] = IO.timer(executionContext)
}
