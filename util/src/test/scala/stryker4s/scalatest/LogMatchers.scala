package stryker4s.scalatest

import ch.qos.logback.classic.Level
import org.scalatest.matchers.{BeMatcher, MatchResult}
import stryker4s.TestAppender

trait LogMatchers {

  class LogMatcher extends BeMatcher[String] {
    def apply(expectedLogMessage: String): MatchResult = {
      MatchResult(
        TestAppender.events.exists(_.getFormattedMessage.contains(expectedLogMessage)),
        s"Log message $expectedLogMessage was expected to be logged but wasn't.",
        s"Log message $expectedLogMessage was logged."
      )
    }
  }

  class LogMatcherWithLevel(expectedLogLevel: Level) extends BeMatcher[String] {
    def apply(expectedLogMessage: String): MatchResult = {
      val result = logMessageExists(expectedLogMessage) && validateLogLevel(expectedLogLevel)

      MatchResult(
        result,
        s"Log message $expectedLogMessage was expected to be logged as $expectedLogLevel but wasn't.",
        s"Log message $expectedLogMessage was logged as $expectedLogLevel."
      )
    }

    private def validateLogLevel(actualLogLevel: Level): Boolean = {
      expectedLogLevel.equals(actualLogLevel)
    }

    private def logMessageExists(expectedLogMessage: String): Boolean = {
      TestAppender.events.exists(_.getFormattedMessage.contains(expectedLogMessage))
    }
  }

  def logged = new LogMatcher
  def loggedAsDebug = new LogMatcherWithLevel(Level.DEBUG)
  def loggedAsInfo = new LogMatcherWithLevel(Level.INFO)
  def loggedAsWarning = new LogMatcherWithLevel(Level.WARN)
  def loggedAsError = new LogMatcherWithLevel(Level.ERROR)
}
