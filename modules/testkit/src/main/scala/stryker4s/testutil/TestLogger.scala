package stryker4s.testutil

import fansi.Str
import stryker4s.log.{Level, Logger}

import java.util.function.Supplier
import scala.collection.mutable.Queue

class TestLogger(printLogs: Boolean) extends Logger {

  private val events = Queue[(Level, String)]()

  def findEvent(msg: String): Option[(Level, String)] = events.find(_._2.contains(msg))

  /** `findEvent`, but ignoring color codes */
  def findEventPlainText(msg: String): Option[(Level, String)] = events.find { case (_, event) =>
    Str(event).plainText.contains(Str(msg).plainText)
  }

  def clear(): Unit = events.clear()

  override def log(level: Level, msg: Supplier[String]): Unit = addToLogs(level, msg.get())

  override def log(level: Level, msg: Supplier[String], e: Throwable): Unit =
    addToLogs(level, s"${msg.get}, ${e.toString()}")

  private def addToLogs(level: Level, msg: => String): Unit = {
    if (printLogs) { println(s"[${level.toString().toUpperCase().padTo(5, ' ')}]: $msg") }
    events.enqueue((level, msg))
  }

  // Always log with colors so we can test for color codes
  override val colorEnabled = true

}
