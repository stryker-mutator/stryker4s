package stryker4s.log

import org.apache.maven.plugin.logging.Log

class MavenMojoLogger(mavenLogger: Log) extends Logger {
  def debug(msg: => String): Unit = if (mavenLogger.isDebugEnabled()) mavenLogger.debug(msg)

  def debug(msg: => String, e: Throwable): Unit = if (mavenLogger.isDebugEnabled()) mavenLogger.debug(msg, e)

  def debug(e: Throwable): Unit = if (mavenLogger.isDebugEnabled()) mavenLogger.debug(e)

  def info(msg: => String): Unit = if (mavenLogger.isInfoEnabled()) mavenLogger.info(msg)

  def info(msg: => String, e: Throwable): Unit = if (mavenLogger.isInfoEnabled()) mavenLogger.info(msg, e)

  def info(e: Throwable): Unit = if (mavenLogger.isInfoEnabled()) mavenLogger.info(e)

  def warn(msg: => String): Unit = if (mavenLogger.isWarnEnabled()) mavenLogger.warn(msg)

  def warn(msg: => String, e: Throwable): Unit = if (mavenLogger.isWarnEnabled()) mavenLogger.warn(msg, e)

  def warn(e: Throwable): Unit = if (mavenLogger.isWarnEnabled()) mavenLogger.warn(e)

  def error(msg: => String): Unit = if (mavenLogger.isErrorEnabled()) mavenLogger.error(msg)

  def error(msg: => String, e: Throwable): Unit = if (mavenLogger.isErrorEnabled()) mavenLogger.error(msg, e)

  def error(e: Throwable): Unit = if (mavenLogger.isErrorEnabled()) mavenLogger.error(e)
}
