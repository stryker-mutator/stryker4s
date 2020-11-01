package stryker4s.scalatest

import org.scalatest.matchers.{BeMatcher, MatchResult}
import org.scalatest.Suite
import stryker4s.testutil.{LogLevel, TestLogger}
import org.scalatest.BeforeAndAfterEach

trait LogMatchers extends BeforeAndAfterEach {
  // Will cause a compile error if LogMatchers is used outside of a ScalaTest Suite
  this: Suite =>

  implicit val testLogger = new TestLogger()

  override protected def afterEach(): Unit = {
    try super.afterEach()
    finally testLogger.clear()
  }

  def loggedAsDebug = new LogMatcherWithLevel(LogLevel.Debug)
  def loggedAsInfo = new LogMatcherWithLevel(LogLevel.Info)
  def loggedAsWarning = new LogMatcherWithLevel(LogLevel.Warn)
  def loggedAsError = new LogMatcherWithLevel(LogLevel.Error)

  private[scalatest] class LogMatcherWithLevel(expectedLogLevel: LogLevel) extends BeMatcher[String] {
    def apply(expectedLogMessage: String): MatchResult = {
      testLogger.findEvent(expectedLogMessage, expectedLogLevel) match {
        case None =>
          MatchResult(
            matches = false,
            s"Log message '$expectedLogMessage' wasn't logged at any level.",
            s"Log message '$expectedLogMessage' was logged as $expectedLogLevel."
          )
        case Some(loggingEvent) =>
          val sameLogLevel = loggingEvent.level == expectedLogLevel

          MatchResult(
            sameLogLevel,
            s"Log message '$expectedLogMessage' was logged but not on correct log level, " +
              s"expected [$expectedLogLevel] actual [${loggingEvent.level}].",
            s"Log message '$expectedLogMessage' was logged as $expectedLogLevel."
          )
      }
    }

  }
}
