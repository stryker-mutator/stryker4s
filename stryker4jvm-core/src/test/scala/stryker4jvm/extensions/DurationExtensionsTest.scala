package stryker4jvm.extensions

import stryker4jvm.extensions.DurationExtensions.*
import stryker4jvm.testutil.Stryker4jvmSuite

import scala.concurrent.duration.*

class DurationExtensionsTest extends Stryker4jvmSuite {
  describe("toHumanReadable") {
    it("should parse 1 second") {
      1.second.toHumanReadable shouldBe "1 second"
    }

    it("should parse 0 duration") {
      0.seconds.toHumanReadable shouldBe "0 seconds"
    }

    it("should parse 1 nanosecond to 0") {
      1.nanosecond.toHumanReadable shouldBe "0 seconds"
    }

    it("should parse 1 ms") {
      1.millisecond.toHumanReadable shouldBe "1 millisecond"
    }

    it("should parse multiple seconds") {
      2.minutes.toHumanReadable shouldBe "2 minutes"
    }

    it("should not include units in the middle that are zero") {
      (3.hours + 0.minutes + 10.seconds).toHumanReadable shouldBe "3 hours and 10 seconds"
    }

    it("should parse a combination of units") {
      (2.days + 3.hours + 2.minutes + 5.seconds + 200.milliseconds).toHumanReadable shouldBe "2 days, 3 hours, 2 minutes, 5 seconds and 200 milliseconds"
    }
  }
}
