package stryker4s.log

import org.slf4j.{Logger => Slf4jInternalLogger, LoggerFactory}

class Slf4jLogger() extends Logger {
  private val slf4jLogger: Slf4jInternalLogger = LoggerFactory.getLogger("Stryker4s")

  def log(level: Level, msg: => String): Unit = level match {
    case Debug => if (slf4jLogger.isDebugEnabled()) slf4jLogger.debug(msg)
    case Info  => if (slf4jLogger.isInfoEnabled()) slf4jLogger.info(msg)
    case Warn  => if (slf4jLogger.isWarnEnabled()) slf4jLogger.warn(msg)
    case Error => if (slf4jLogger.isErrorEnabled()) slf4jLogger.error(msg)
  }

  def log(level: Level, msg: => String, e: => Throwable): Unit = level match {
    case Debug => if (slf4jLogger.isDebugEnabled()) slf4jLogger.debug(msg, e)
    case Info  => if (slf4jLogger.isInfoEnabled()) slf4jLogger.info(msg, e)
    case Warn  => if (slf4jLogger.isWarnEnabled()) slf4jLogger.warn(msg, e)
    case Error => if (slf4jLogger.isErrorEnabled()) slf4jLogger.error(msg, e)
  }

  def log(level: Level, e: Throwable): Unit = level match {
    case Debug => if (slf4jLogger.isDebugEnabled()) slf4jLogger.debug(e.getLocalizedMessage(), e)
    case Info  => if (slf4jLogger.isInfoEnabled()) slf4jLogger.info(e.getLocalizedMessage(), e)
    case Warn  => if (slf4jLogger.isWarnEnabled()) slf4jLogger.warn(e.getLocalizedMessage(), e)
    case Error => if (slf4jLogger.isErrorEnabled()) slf4jLogger.error(e.getLocalizedMessage(), e)
  }

}
