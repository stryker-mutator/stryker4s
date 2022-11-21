package stryker4jvm.testutil

import fansi.Str
import stryker4jvm.logging.{Level, Logger}

import scala.collection.mutable.Queue

class TestLogger(printLogs: Boolean) extends Logger {

  private val events = Queue[(Level, String)]()

  def findEvent(msg: String): Option[(Level, String)] = events.find(_._2.contains(msg))

  /** `findEvent`, but ignoring color codes */
  def findEventPlainText(msg: String): Option[(Level, String)] = events.find { case (_, event) =>
    Str(event).plainText.contains(Str(msg).plainText)
  }

  def clear(): Unit = events.clear()

  def log(level: Level, msg: => String): Unit = addToLogs(level, msg)

  def log(level: Level, msg: => String, e: => Throwable): Unit = addToLogs(level, s"$msg, ${e.toString()}")

  def log(level: Level, e: Throwable): Unit = addToLogs(level, e.toString())

  private def addToLogs(level: Level, msg: => String): Unit = {
    if (printLogs) { println(s"[${level.toString().toUpperCase()}]: $msg") }
    events.enqueue((level, msg))
  }

  // Always log with colors so we can test for color codes
  override val colorEnabled = true
}
