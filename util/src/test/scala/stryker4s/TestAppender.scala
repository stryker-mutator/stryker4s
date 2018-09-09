package stryker4s

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object TestAppender {
  val events: mutable.ListBuffer[ILoggingEvent] = ListBuffer.empty

  /**
    * Remove all previous logged events for a specific class.
    */
  def reset(implicit loggerClassName: String): Unit = {
    events --= events.filterNot(event => event.getLoggerName.contains(loggerClassName))
  }
}

class TestAppender extends AppenderBase[ILoggingEvent] {

  override def append(eventObject: ILoggingEvent): Unit = {
    TestAppender.events += eventObject
  }
}
