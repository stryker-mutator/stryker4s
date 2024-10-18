package stryker4s.extension

import stryker4s.testkit.Stryker4sSuite

import scala.concurrent.duration.*

import DurationExtensions.*

class DurationExtensionsTest extends Stryker4sSuite {
  describe("toHumanReadable") {
    test("should parse 1 second") {
      assertEquals(1.second.toHumanReadable, "1 second")
    }

    test("should parse 0 duration") {
      assertEquals(0.seconds.toHumanReadable, "0 seconds")
    }

    test("should parse 1 nanosecond to 0") {
      assertEquals(1.nanosecond.toHumanReadable, "0 seconds")
    }

    test("should parse 1 ms") {
      assertEquals(1.millisecond.toHumanReadable, "1 millisecond")
    }

    test("should parse multiple seconds") {
      assertEquals(2.minutes.toHumanReadable, "2 minutes")
    }

    test("should not include units in the middle that are zero") {
      assertEquals((3.hours + 0.minutes + 10.seconds).toHumanReadable, "3 hours and 10 seconds")
    }

    test("should parse a combination of units") {
      assertEquals(
        (2.days + 3.hours + 2.minutes + 5.seconds + 200.milliseconds).toHumanReadable,
        "2 days, 3 hours, 2 minutes, 5 seconds and 200 milliseconds"
      )
    }
  }
}
