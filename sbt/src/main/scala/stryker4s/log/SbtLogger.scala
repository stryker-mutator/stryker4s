package stryker4s.log

import sbt.{Logger => SbtInternalLogger}

class SbtLogger(sbtLogger: SbtInternalLogger) extends Logger {

  def debug(msg: => String): Unit = sbtLogger.debug(msg)

  def debug(msg: => String, e: Throwable): Unit = {
    sbtLogger.debug(msg)
    sbtLogger.trace(e)
  }

  def debug(e: Throwable): Unit = sbtLogger.trace(e)

  def info(msg: => String): Unit = sbtLogger.info(msg)

  def info(msg: => String, e: Throwable): Unit = {
    sbtLogger.info(msg)
    sbtLogger.trace(e)
  }

  def info(e: Throwable): Unit = sbtLogger.trace(e)

  def warn(msg: => String): Unit = sbtLogger.warn(msg)

  def warn(msg: => String, e: Throwable): Unit = {
    sbtLogger.warn(msg)
    sbtLogger.trace(e)
  }

  def warn(e: Throwable): Unit = sbtLogger.trace(e)

  def error(msg: => String): Unit = sbtLogger.error(msg)

  def error(msg: => String, e: Throwable): Unit = {
    sbtLogger.warn(msg)
    sbtLogger.trace(e)
  }

  def error(e: Throwable): Unit = sbtLogger.trace(e)

}
