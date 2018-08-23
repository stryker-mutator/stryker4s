package stryker4s
import java.io.OutputStream

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.{AppenderBase, ConsoleAppender}
import org.slf4j.event.LoggingEvent

object TestAppender {
  var events: List[ILoggingEvent] = List()
}

class TestAppender extends AppenderBase[ILoggingEvent] {

  override def append(eventObject: ILoggingEvent): Unit = TestAppender.events = TestAppender.events :+ eventObject
}
