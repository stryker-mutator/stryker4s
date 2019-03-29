package stryker4s.testutil

import org.apache.logging.log4j.core._
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.logging.log4j.core.config.plugins._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object TestAppender {

  val events: mutable.Map[String, ListBuffer[LogEvent]] =
    new mutable.HashMap[String, ListBuffer[LogEvent]]().withDefaultValue(ListBuffer.empty)

  /**
    * Remove all previous logged events for a specific class.
    */
  def reset(implicit threadName: String): Unit = events(threadName).clear()

  @PluginFactory def createAppender(@PluginAttribute("name") name: String,
                                    @PluginElement("Filter") filter: Filter): TestAppender =
    new TestAppender(name, filter)
}

@Plugin(name = "TestAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
class TestAppender(name: String, filter: Filter) extends AbstractAppender(name, filter, null, true, Property.EMPTY_ARRAY) {

  override def append(eventObject: LogEvent): Unit = {
    // Needs to call .toImmutable because the same object is given every time, with only a mutated message
    TestAppender.events(eventObject.getThreadName) += eventObject.toImmutable
  }
}
