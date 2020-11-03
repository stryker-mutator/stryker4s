package stryker4s.run

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.LoggerContext
import stryker4s.command.Stryker4sArgumentHandler
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

class Stryker4sArgumentHandlerTest extends Stryker4sSuite with LogMatchers {
  describe("parseArgs") {
    it("should set the default logLevel to INFO") {
      Stryker4sArgumentHandler.handleArgs(Seq.empty)

      "Set logging level to INFO" shouldBe loggedAsInfo
      LoggerContext.getContext(false).getRootLogger.getLevel shouldEqual Level.INFO
    }

    val levels = Seq(
      "off",
      "trace",
      "info",
      "warn",
      "error",
      "fatal",
      "all"
    )

    levels.foreach { level =>
      it(s"should parse $level to ${level.toUpperCase()}") {
        Stryker4sArgumentHandler.handleArgs(Seq(s"--$level"))
        s"Set logging level to ${level.toUpperCase}" shouldBe loggedAsInfo
        LoggerContext.getContext(false).getRootLogger.getLevel.toString shouldEqual level.toUpperCase
      }
    }

    it("should parse regardless of casing") {
      Stryker4sArgumentHandler.handleArgs(Seq("--DeBUg"))
      "Set logging level to DEBUG" shouldBe loggedAsInfo
      LoggerContext.getContext(false).getRootLogger.getLevel shouldEqual Level.DEBUG
    }
  }
}
