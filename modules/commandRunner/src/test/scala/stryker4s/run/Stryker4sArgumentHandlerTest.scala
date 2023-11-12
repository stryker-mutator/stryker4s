package stryker4s.run

import org.slf4j.event.Level
import org.slf4j.simple.SimpleLogger
import stryker4s.command.Stryker4sArgumentHandler
import stryker4s.testkit.{LogMatchers, Stryker4sSuite}

class Stryker4sArgumentHandlerTest extends Stryker4sSuite with LogMatchers {
  test("parseArgs should set the default logLevel to INFO") {
    val logString = Stryker4sArgumentHandler.handleArgs(Seq.empty)

    assertNoDiff(logString, "Set logging level to INFO")
    assertNoDiff(sys.props(SimpleLogger.DEFAULT_LOG_LEVEL_KEY), Level.INFO.toString())
  }

  val levels = Seq(
    "trace",
    "info",
    "warn",
    "error"
  )

  levels.foreach { level =>
    test(s"parseArgs should parse $level to ${level.toUpperCase()}") {
      val logString = Stryker4sArgumentHandler.handleArgs(Seq(s"--$level"))

      assertNoDiff(logString, s"Set logging level to ${level.toUpperCase}")
      assertNoDiff(sys.props(SimpleLogger.DEFAULT_LOG_LEVEL_KEY), level.toUpperCase)
    }
  }

  test("parseArgs should parse regardless of casing") {
    val logString = Stryker4sArgumentHandler.handleArgs(Seq("--DeBUg"))

    assertNoDiff(logString, "Set logging level to DEBUG")
    assertNoDiff(sys.props(SimpleLogger.DEFAULT_LOG_LEVEL_KEY), Level.DEBUG.toString())
  }

}
