package stryker4s.scalatest

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.LogEvent
import org.scalatest.matchers.{BeMatcher, MatchResult}
import stryker4s.{Stryker4sSuite, TestAppender}

trait LogMatchers {
  // Will cause a compile error if LogMatchers is used without Stryker4sSuite
  this: Stryker4sSuite =>

  def loggedAsDebug = new LogMatcherWithLevel(Level.DEBUG)
  def loggedAsInfo = new LogMatcherWithLevel(Level.INFO)
  def loggedAsWarning = new LogMatcherWithLevel(Level.WARN)
  def loggedAsError = new LogMatcherWithLevel(Level.ERROR)

  override def afterEach(): Unit = TestAppender.reset

  /**
    * The name of the current thread. AKA the current test (class) being executed
    */
  private implicit val threadName: String = Thread.currentThread().getName

  protected class LogMatcherWithLevel(expectedLogLevel: Level)(implicit threadName: String)
      extends BeMatcher[String] {
    def apply(expectedLogMessage: String): MatchResult = {
      getLoggingEventWithLogMessage(expectedLogMessage) match {
        case None =>
          MatchResult(
            matches = false,
            s"Log message '$expectedLogMessage' wasn't logged at any level.",
            s"Log message '$expectedLogMessage' was logged as $expectedLogLevel."
          )
        case Some(loggingEvent) =>
          val result = validateLogLevel(loggingEvent.getLevel, expectedLogLevel)

          MatchResult(
            result,
            s"Log message '$expectedLogMessage' was logged but not on correct log level, " +
              s"expected [$expectedLogLevel] actual [${loggingEvent.getLevel}].",
            s"Log message '$expectedLogMessage' was logged as $expectedLogLevel."
          )
      }
    }

    private def validateLogLevel(actualLogLevel: Level, expectedLogLevel: Level): Boolean = {
      expectedLogLevel.equals(actualLogLevel)
    }

    private def getLoggingEventWithLogMessage(expectedLogMessage: String): Option[LogEvent] = {
      TestAppender
        .events(threadName)
        .find(_.getMessage.getFormattedMessage.contains(expectedLogMessage))
    }
  }
}
