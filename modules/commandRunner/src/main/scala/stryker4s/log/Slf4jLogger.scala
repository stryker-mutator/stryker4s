package stryker4s.log

import org.slf4j.simple.SimpleLoggerFactory
import org.slf4j.Logger as Slf4jInternalLogger

class Slf4jLogger() extends Logger {
  private val slf4jLogger: Slf4jInternalLogger = new SimpleLoggerFactory().getLogger("Stryker4s")

  override def log(level: Level, msg: => String): Unit =
    doLog(level)(
      slf4jLogger.debug(msg),
      slf4jLogger.info(msg),
      slf4jLogger.warn(msg),
      slf4jLogger.error(msg)
    )

  override def log(level: Level, msg: => String, e: => Throwable): Unit = doLog(level)(
    slf4jLogger.debug(msg, e),
    slf4jLogger.info(msg, e),
    slf4jLogger.warn(msg, e),
    slf4jLogger.error(msg, e)
  )

  private def doLog(level: Level)(
      onDebug: => Unit,
      onInfo: => Unit,
      onWarn: => Unit,
      onError: => Unit
  ): Unit = level match {
    case Level.Debug => if (slf4jLogger.isDebugEnabled()) onDebug
    case Level.Info  => if (slf4jLogger.isInfoEnabled()) onInfo
    case Level.Warn  => if (slf4jLogger.isWarnEnabled()) onWarn
    case Level.Error => if (slf4jLogger.isErrorEnabled()) onError
  }

}
