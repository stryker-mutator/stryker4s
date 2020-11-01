package stryker4s.testutil

import stryker4s.log.Logger

import LogLevel._

class TestLogger extends Logger {

  private val events = scala.collection.mutable.Queue[LogEvent]()

  def findEvent(msg: String, level: LogLevel): Option[LogEvent] =
    events
      .find(event => event.level.equals(level) && event.msg.contains(msg))

  def clear(): Unit = events.clear()

  def debug(msg: => String): Unit = events.enqueue(LogEvent(Debug, msg))

  def debug(msg: => String, e: Throwable): Unit = events.enqueue(LogEvent(Debug, s"$msg, ${e.toString()}"))

  def debug(e: Throwable): Unit = events.enqueue(LogEvent(Debug, e.toString()))

  def info(msg: => String): Unit = events.enqueue(LogEvent(Info, msg))

  def info(msg: => String, e: Throwable): Unit = events.enqueue(LogEvent(Info, s"$msg, ${e.toString()}"))

  def info(e: Throwable): Unit = events.enqueue(LogEvent(Info, e.toString()))

  def warn(msg: => String): Unit = events.enqueue(LogEvent(Warn, msg))

  def warn(msg: => String, e: Throwable): Unit = events.enqueue(LogEvent(Warn, s"$msg, ${e.toString()}"))

  def warn(e: Throwable): Unit = events.enqueue(LogEvent(Warn, e.toString()))

  def error(msg: => String): Unit = events.enqueue(LogEvent(Error, msg))

  def error(msg: => String, e: Throwable): Unit = events.enqueue(LogEvent(Error, s"$msg, ${e.toString()}"))

  def error(e: Throwable): Unit = events.enqueue(LogEvent(Error, e.toString()))

}

case class LogEvent(level: LogLevel, msg: String)
