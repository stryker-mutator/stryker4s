package stryker4s.log

import org.apache.maven.plugin.logging.Log

class MavenMojoLogger(mavenLogger: Log) extends Logger {

  def log(level: Level, msg: => String): Unit = level match {
    case Debug => if (mavenLogger.isDebugEnabled()) mavenLogger.debug(msg)
    case Info  => if (mavenLogger.isInfoEnabled()) mavenLogger.info(msg)
    case Warn  => if (mavenLogger.isWarnEnabled()) mavenLogger.warn(msg)
    case Error => if (mavenLogger.isErrorEnabled()) mavenLogger.error(msg)
  }

  def log(level: Level, msg: => String, e: => Throwable): Unit = level match {
    case Debug => if (mavenLogger.isDebugEnabled()) mavenLogger.debug(msg, e)
    case Info  => if (mavenLogger.isInfoEnabled()) mavenLogger.info(msg, e)
    case Warn  => if (mavenLogger.isWarnEnabled()) mavenLogger.warn(msg, e)
    case Error => if (mavenLogger.isErrorEnabled()) mavenLogger.error(msg, e)
  }

  def log(level: Level, e: Throwable): Unit = level match {
    case Debug => if (mavenLogger.isDebugEnabled()) mavenLogger.debug(e)
    case Info  => if (mavenLogger.isInfoEnabled()) mavenLogger.info(e)
    case Warn  => if (mavenLogger.isWarnEnabled()) mavenLogger.warn(e)
    case Error => if (mavenLogger.isErrorEnabled()) mavenLogger.error(e)
  }

}
