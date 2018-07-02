package stryker4s.scalatest

import uk.org.lidalia.slf4jtest.LoggingEvent._
import uk.org.lidalia.slf4jtest.{LoggingEvent, TestLogger, TestLoggerFactory}

trait LoggerTests {
  def loggerOf(obj: Any): TestLogger = TestLoggerFactory.getTestLogger(obj.getClass)

  def debugLog(message: String): LoggingEvent = debug(message)

  def infoLog(message: String): LoggingEvent = info(message)

  def warnLog(message: String): LoggingEvent = warn(message)

  def errorLog(message: String): LoggingEvent = error(message)

}
