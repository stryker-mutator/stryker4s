package stryker4s.testutil

import stryker4s.log.{Level, Logger}

class NoopLogger extends Logger {

  override def log(level: Level, msg: => String): Unit = {}

  override def log(level: Level, msg: => String, e: => Throwable): Unit = {}

}
