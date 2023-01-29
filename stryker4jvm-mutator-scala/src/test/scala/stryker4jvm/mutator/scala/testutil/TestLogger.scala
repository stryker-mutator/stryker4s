package stryker4jvm.mutator.scala.testutil

import stryker4jvm.core.logging.{LogLevel, Logger}

class TestLogger(var logs: List[String] = List.empty) extends Logger {
  override def log(logLevel: LogLevel, s: String): Unit = {
    logs = logs :+ s
  }

  override def log(logLevel: LogLevel, s: String, throwable: Throwable): Unit = ???

  override def log(logLevel: LogLevel, throwable: Throwable): Unit = ???

  def clear(): Unit = {
    logs = List.empty
  }
}
