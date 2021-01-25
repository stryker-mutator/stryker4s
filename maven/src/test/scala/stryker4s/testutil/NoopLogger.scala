package stryker4s.testutil

import stryker4s.log.{Level, Logger}

class NoopLogger extends Logger {

  def log(level: Level, msg: => String): Unit = {}

  def log(level: Level, msg: => String, e: => Throwable): Unit = {}

  def log(level: Level, e: Throwable): Unit = {}

}
