package stryker4s.log

import org.slf4j.{Logger => Slf4jInternalLogger, LoggerFactory}

class Slf4jLogger() extends Logger {
  private val slf4jLogger: Slf4jInternalLogger = LoggerFactory.getLogger("Stryker4s")

  def debug(msg: => String): Unit = if (slf4jLogger.isDebugEnabled()) slf4jLogger.debug(msg)

  def debug(msg: => String, e: Throwable): Unit = if (slf4jLogger.isDebugEnabled()) slf4jLogger.debug(msg, e)

  def debug(e: Throwable): Unit = if (slf4jLogger.isDebugEnabled()) slf4jLogger.debug(e.getLocalizedMessage, e)

  def info(msg: => String): Unit = if (slf4jLogger.isInfoEnabled()) slf4jLogger.info(msg)

  def info(msg: => String, e: Throwable): Unit = if (slf4jLogger.isInfoEnabled()) slf4jLogger.info(msg, e)

  def info(e: Throwable): Unit = if (slf4jLogger.isInfoEnabled()) slf4jLogger.info(e.getLocalizedMessage(), e)

  def warn(msg: => String): Unit = if (slf4jLogger.isWarnEnabled()) slf4jLogger.warn(msg)

  def warn(msg: => String, e: Throwable): Unit = if (slf4jLogger.isWarnEnabled()) slf4jLogger.warn(msg, e)

  def warn(e: Throwable): Unit = if (slf4jLogger.isWarnEnabled()) slf4jLogger.warn(e.getLocalizedMessage(), e)

  def error(msg: => String): Unit = if (slf4jLogger.isErrorEnabled()) slf4jLogger.error(msg)

  def error(msg: => String, e: Throwable): Unit = if (slf4jLogger.isErrorEnabled()) slf4jLogger.error(msg, e)

  def error(e: Throwable): Unit = if (slf4jLogger.isErrorEnabled()) slf4jLogger.error(e.getLocalizedMessage(), e)

}
