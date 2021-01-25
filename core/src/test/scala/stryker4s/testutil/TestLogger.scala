package stryker4s.testutil

import scala.collection.mutable.Queue

import stryker4s.log.{Level, Logger}

class TestLogger(printLogs: Boolean) extends Logger {

  private val events = Queue[(Level, String)]()

  def findEvent(msg: String): Option[(Level, String)] = events.find(_._2.contains(msg))

  def clear(): Unit = events.clear()

  def log(level: Level, msg: => String): Unit = addToLogs(level, msg)

  def log(level: Level, msg: => String, e: => Throwable): Unit = addToLogs(level, s"$msg, ${e.toString()}")

  def log(level: Level, e: Throwable): Unit = addToLogs(level, e.toString())

  private def addToLogs(level: Level, msg: => String): Unit = {
    if (printLogs) { println(s"[${level.toString().toUpperCase()}]: $msg") }
    events.enqueue((level, msg))
  }

}
