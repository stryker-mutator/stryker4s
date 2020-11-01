package stryker4s.testutil

import stryker4s.log.Logger

class NoopLogger extends Logger {
  def debug(msg: => String): Unit = {}

  def debug(msg: => String, e: Throwable): Unit = {}

  def debug(e: Throwable): Unit = {}

  def info(msg: => String): Unit = {}

  def info(msg: => String, e: Throwable): Unit = {}

  def info(e: Throwable): Unit = {}

  def warn(msg: => String): Unit = {}

  def warn(msg: => String, e: Throwable): Unit = {}

  def warn(e: Throwable): Unit = {}

  def error(msg: => String): Unit = {}

  def error(msg: => String, e: Throwable): Unit = {}

  def error(e: Throwable): Unit = {}
}
