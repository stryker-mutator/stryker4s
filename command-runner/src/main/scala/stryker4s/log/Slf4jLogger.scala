package stryker4s.log

import org.slf4j.impl.SimpleLoggerFactory
import org.slf4j.Logger as Slf4jInternalLogger

class Slf4jLogger() extends Logger {
  private val slf4jLogger: Slf4jInternalLogger = new SimpleLoggerFactory.getLogger("Stryker4s")

  def log(level: Level, msg: => String): Unit =
    doLog(level)(
      slf4jLogger.debug(msg),
      slf4jLogger.info(msg),
      slf4jLogger.warn(msg),
      slf4jLogger.error(msg)
    )

  def log(level: Level, msg: => String, e: => Throwable): Unit = doLog(level)(
    slf4jLogger.debug(msg, e),
    slf4jLogger.info(msg, e),
    slf4jLogger.warn(msg, e),
    slf4jLogger.error(msg, e)
  )

  def log(level: Level, e: Throwable): Unit = doLog(level)(
    slf4jLogger.debug(e.getLocalizedMessage(), e),
    slf4jLogger.info(e.getLocalizedMessage(), e),
    slf4jLogger.warn(e.getLocalizedMessage(), e),
    slf4jLogger.error(e.getLocalizedMessage(), e)
  )

  private def doLog(level: Level)(
      onDebug: => Unit,
      onInfo: => Unit,
      onWarn: => Unit,
      onError: => Unit
  ): Unit = level match {
    case Debug => if (slf4jLogger.isDebugEnabled()) onDebug
    case Info  => if (slf4jLogger.isInfoEnabled()) onInfo
    case Warn  => if (slf4jLogger.isWarnEnabled()) onWarn
    case Error => if (slf4jLogger.isErrorEnabled()) onError
  }

}
