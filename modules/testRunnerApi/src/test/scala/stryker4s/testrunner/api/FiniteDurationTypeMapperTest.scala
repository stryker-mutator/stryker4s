package stryker4s.testrunner.api

import munit.FunSuite

import scala.concurrent.duration.*

class FiniteDurationTypeMapperTest extends FunSuite {
  val sut = finiteDurationTypeMapper

  test("maps a FiniteDuration to nanoseconds") {
    assertEquals(sut.toBase(1500.millis), 1500000000L)
  }

  test("round-trips through the base type") {
    Seq(0.seconds, 1.nano, 2.hours + 3.nanos, (-1500).millis).foreach { d =>
      assertEquals(sut.toCustom(sut.toBase(d)), d)
    }
  }

  test("round-trips CoverageTestRunResult through the wire format") {
    val result = CoverageTestRunResult(isSuccessful = true, CoverageTestNameMap(), 1234567890.nanos)

    assertEquals(CoverageTestRunResult.parseFrom(result.toByteArray), result)
  }
}
