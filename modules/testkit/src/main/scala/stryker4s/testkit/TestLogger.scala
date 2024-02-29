package stryker4s.testkit

import fansi.Str
import stryker4s.log.{Level, Logger}

import scala.collection.mutable.Buffer

protected[testkit] class TestLogger(printLogs: Boolean) extends Logger {

  private val events = Buffer.empty[(Level, String)]

  def findEvent(msg: String): Option[(Level, String)] = events.find(_._2.contains(msg))

  /** `findEvent`, but ignoring color codes */
  def findEventPlainText(msg: String): Option[(Level, String)] = events.find { case (_, event) =>
    Str(event).plainText.contains(Str(msg).plainText)
  }

  def clear(): Unit = events.clear()

  override def log(level: Level, msg: => String): Unit = addToLogs(level, msg)

  override def log(level: Level, msg: => String, e: => Throwable): Unit =
    addToLogs(level, s"$msg, ${e.toString()}")

  def printAllLogs(): Unit = {
    events.foreach { case (level, msg) => printLog(level, msg) }
  }

  private def addToLogs(level: Level, msg: => String): Unit = {
    if (printLogs) { printLog(level, msg) }
    events.append((level, msg))
  }

  private def printLog(level: Level, msg: => String): Unit = {
    println(s"[${level.toString().toUpperCase().padTo(5, ' ')}]: $msg")
  }

  // Always log with colors so we can test for color codes
  override val colorEnabled = true

}
