package stryker4s.scalatest

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.{BeMatcher, MatchResult}
import stryker4s.{Stryker4sSuite, TestAppender}

trait LogMatchers extends BeforeAndAfterEach {
  // Causes a compile error if LogMatchers is used without Stryker4sSuite
  this: Stryker4sSuite =>

  def loggedAsDebug = new LogMatcherWithLevel(Level.DEBUG)
  def loggedAsInfo = new LogMatcherWithLevel(Level.INFO)
  def loggedAsWarning = new LogMatcherWithLevel(Level.WARN)
  def loggedAsError = new LogMatcherWithLevel(Level.ERROR)

  override def afterEach(): Unit = TestAppender.reset

  /**
    * The className of the system under test.
    * Is done by getting the executing test class and stripping off 'test'.
    */
  implicit val className: String = getClass.getCanonicalName.replace("Test", "")

  class LogMatcherWithLevel(expectedLogLevel: Level)(implicit loggerClassName: String)
      extends BeMatcher[String] {
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

    private def validateLogLevel(actualLogLevel: Level, expectedLogLevel: Level): Boolean = {
      expectedLogLevel.equals(actualLogLevel)
    }

    private def getLoggingEventWithLogMessage(expectedLogMessage: String): Option[ILoggingEvent] = {
      TestAppender.events
        .filter(logEvent => logEvent.getLoggerName.contains(loggerClassName))
        .find(_.getFormattedMessage.contains(expectedLogMessage))
    }
  }
}
