package stryker4jvm.testutil

import fansi.Str
import stryker4jvm.core.logging.{LogLevel, Logger}

import scala.collection.mutable.Queue

class TestLogger(printLogs: Boolean) extends Logger {

  private val events = Queue[(LogLevel, String)]()

  def findEvent(msg: String): Option[(LogLevel, String)] = events.find(_._2.contains(msg))

  /** `findEvent`, but ignoring color codes */
  def findEventPlainText(msg: String): Option[(LogLevel, String)] = events.find { case (_, event) =>
    Str(event).plainText.contains(Str(msg).plainText)
  }

  def clear(): Unit = events.clear()
  // TODO: Difference between msg: => String and msg: String in log
  def log(level: LogLevel, msg: => String): Unit = addToLogs(level, msg)

  override def log(logLevel: LogLevel, msg: String): Unit = addToLogs(logLevel, msg)

  def log(level: LogLevel, msg: => String, e: => Throwable): Unit = addToLogs(level, s"$msg, ${e.toString()}")

  override def log(logLevel: LogLevel, msg: String, e: Throwable): Unit = addToLogs(logLevel, s"$msg, ${e.toString()}")

  def log(level: LogLevel, e: Throwable): Unit = addToLogs(level, e.toString())

  private def addToLogs(level: LogLevel, msg: => String): Unit = {
    if (printLogs) { println(s"[${level.toString().toUpperCase()}]: $msg") }
    events.enqueue((level, msg))
  }

  override def determineColorEnabled = true
}
