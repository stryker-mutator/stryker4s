package stryker4s

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase

import scala.collection.mutable

object TestAppender {
  val events: mutable.MutableList[ILoggingEvent] = mutable.MutableList.empty

  /**
    * Remove all previous logged events.
    */
  def reset(): Unit = {
    events.clear()
  }
}

class TestAppender extends AppenderBase[ILoggingEvent] {

  override def append(eventObject: ILoggingEvent): Unit = {
    eventObject.getLoggerName

    TestAppender.events += eventObject
  }
}
