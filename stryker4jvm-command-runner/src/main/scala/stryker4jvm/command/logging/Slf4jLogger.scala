package stryker4jvm.command.logging

import org.slf4j.Logger as Slf4jInternalLogger
import org.slf4j.simple.SimpleLoggerFactory
import stryker4jvm.core.logging.LogLevel
import stryker4jvm.logging.FansiLogger

class FansiSlf4jLogger() extends FansiLogger(new Slf4jLogger())

private class Slf4jLogger() extends stryker4jvm.core.logging.Logger {
  private val slf4jLogger: Slf4jInternalLogger = new SimpleLoggerFactory().getLogger("Stryker4jvm")

  def log(level: LogLevel, msg: String): Unit =
    doLog(level)(
      slf4jLogger.debug(msg),
      slf4jLogger.info(msg),
      slf4jLogger.warn(msg),
      slf4jLogger.error(msg)
    )

  def log(level: LogLevel, msg: String, e: Throwable): Unit = doLog(level)(
    slf4jLogger.debug(msg, e),
    slf4jLogger.info(msg, e),
    slf4jLogger.warn(msg, e),
    slf4jLogger.error(msg, e)
  )

  def log(level: LogLevel, e: Throwable): Unit = doLog(level)(
    slf4jLogger.debug(e.getLocalizedMessage(), e),
    slf4jLogger.info(e.getLocalizedMessage(), e),
    slf4jLogger.warn(e.getLocalizedMessage(), e),
    slf4jLogger.error(e.getLocalizedMessage(), e)
  )

  private def doLog(level: LogLevel)(
      onDebug: => Unit,
      onInfo: => Unit,
      onWarn: => Unit,
      onError: => Unit
  ): Unit = level match {
    case LogLevel.Debug => if (slf4jLogger.isDebugEnabled()) onDebug
    case LogLevel.Info  => if (slf4jLogger.isInfoEnabled()) onInfo
    case LogLevel.Warn  => if (slf4jLogger.isWarnEnabled()) onWarn
    case LogLevel.Error => if (slf4jLogger.isErrorEnabled()) onError
  }
}
