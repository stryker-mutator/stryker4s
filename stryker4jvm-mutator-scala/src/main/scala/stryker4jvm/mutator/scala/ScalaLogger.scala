package stryker4jvm.mutator.scala

import stryker4jvm.core.logging.Logger
import stryker4jvm.core.logging.LogLevel

class ScalaLogger extends Logger {

  override def log(x$1: LogLevel, x$2: String): Unit = { println(x$2) }

  override def log(x$1: LogLevel, x$2: String, x$3: Throwable): Unit = { println(x$2) }

  override def log(x$1: LogLevel, x$2: Throwable): Unit = {}

}
