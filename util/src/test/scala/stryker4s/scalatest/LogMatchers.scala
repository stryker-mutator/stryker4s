package stryker4s.scalatest

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import org.scalatest.matchers.{BeMatcher, MatchResult}
import stryker4s.TestAppender

trait LogMatchers {

  class LogMatcherWithLevel[A](expectedLogLevel: Level)(implicit loggerClassName: String) extends BeMatcher[String] {
    def apply(expectedLogMessage: String): MatchResult = {
        getLoggingEventWithLogMessage(expectedLogMessage) match {
          case None =>
            MatchResult(
              matches = false,
              s"Log message $expectedLogMessage wasn't logged at any level.",
              s"Log message $expectedLogMessage was logged as $expectedLogLevel."
            )
          case Some(loggingEvent) =>
            val result = validateLogLevel(loggingEvent.getLevel, expectedLogLevel)

            MatchResult(
              result,
              s"Log message $expectedLogMessage was logged but not on correct log level, " +
                s"expected [$expectedLogLevel] actual [${loggingEvent.getLevel}].",
              s"Log message $expectedLogMessage was logged as $expectedLogLevel."
            )
        }
    }
  }

  def loggedAsDebug(implicit loggerClassName: String) = new LogMatcherWithLevel(Level.DEBUG)
  def loggedAsInfo(implicit loggerClassName: String) = new LogMatcherWithLevel(Level.INFO)
  def loggedAsWarning(implicit loggerClassName: String) = new LogMatcherWithLevel(Level.WARN)
  def loggedAsError(implicit loggerClassName: String) = new LogMatcherWithLevel(Level.ERROR)

  private[this] def validateLogLevel(actualLogLevel: Level, expectedLogLevel: Level): Boolean = {
    expectedLogLevel.equals(actualLogLevel)
  }

  private[this] def getLoggingEventWithLogMessage(expectedLogMessage: String)(implicit loggerClassName: String): Option[ILoggingEvent] = {
    TestAppender.events
      .filter(logEvent => logEvent.getLoggerName.contains(loggerClassName))
      .find(_.getFormattedMessage.contains(expectedLogMessage))
  }
}
