package stryker4s.testutil

import stryker4jvm.core.logging.{LogLevel, Logger}

class NoopLogger extends Logger {

  override def log(level: LogLevel, msg: String): Unit = {}

  override def log(level: LogLevel, msg: String, e: Throwable): Unit = {}

  override def log(level: LogLevel, e: Throwable): Unit = {}
}
