package stryker4s.run

import org.slf4j.event.Level
import org.slf4j.simple.SimpleLogger
import stryker4s.command.Stryker4sArgumentHandler
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

class Stryker4sArgumentHandlerTest extends Stryker4sSuite with LogMatchers {
  describe("parseArgs") {
    it("should set the default logLevel to INFO") {
      val logString = Stryker4sArgumentHandler.handleArgs(Seq.empty)

      logString shouldBe "Set logging level to INFO"
      sys.props(SimpleLogger.DEFAULT_LOG_LEVEL_KEY) shouldEqual Level.INFO.toString()
    }

    val levels = Seq(
      "trace",
      "info",
      "warn",
      "error"
    )

    levels.foreach { level =>
      it(s"should parse $level to ${level.toUpperCase()}") {
        val logString = Stryker4sArgumentHandler.handleArgs(Seq(s"--$level"))

        logString shouldBe s"Set logging level to ${level.toUpperCase}"
        sys.props(SimpleLogger.DEFAULT_LOG_LEVEL_KEY) shouldEqual level.toUpperCase
      }
    }

    it("should parse regardless of casing") {
      val logString = Stryker4sArgumentHandler.handleArgs(Seq("--DeBUg"))

      logString shouldBe "Set logging level to DEBUG"
      sys.props(SimpleLogger.DEFAULT_LOG_LEVEL_KEY) shouldEqual Level.DEBUG.toString()
    }
  }
}
