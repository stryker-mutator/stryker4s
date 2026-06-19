package stryker4s.log

import org.apache.maven.plugin.logging.Log
import stryker4s.testkit.Stryker4sSuite

import scala.collection.mutable

class MavenMojoLoggerTest extends Stryker4sSuite {

  /** A fake Maven [[Log]] that records logged messages per level and lets each level be enabled/disabled. */
  private class FakeMavenLog(
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ) extends Log {
    val debugMessages: mutable.Buffer[String] = mutable.Buffer.empty
    val infoMessages: mutable.Buffer[String] = mutable.Buffer.empty
    val warnMessages: mutable.Buffer[String] = mutable.Buffer.empty
    val errorMessages: mutable.Buffer[String] = mutable.Buffer.empty

    override def isDebugEnabled(): Boolean = debugEnabled
    override def isInfoEnabled(): Boolean = infoEnabled
    override def isWarnEnabled(): Boolean = warnEnabled
    override def isErrorEnabled(): Boolean = errorEnabled

    override def debug(msg: CharSequence): Unit = { debugMessages += msg.toString; () }
    override def debug(t: Throwable): Unit = ()
    override def debug(msg: CharSequence, t: Throwable): Unit = { debugMessages += msg.toString; () }
    override def info(msg: CharSequence): Unit = { infoMessages += msg.toString; () }
    override def info(t: Throwable): Unit = ()
    override def info(msg: CharSequence, t: Throwable): Unit = { infoMessages += msg.toString; () }
    override def warn(msg: CharSequence): Unit = { warnMessages += msg.toString; () }
    override def warn(t: Throwable): Unit = ()
    override def warn(msg: CharSequence, t: Throwable): Unit = { warnMessages += msg.toString; () }
    override def error(msg: CharSequence): Unit = { errorMessages += msg.toString; () }
    override def error(t: Throwable): Unit = ()
    override def error(msg: CharSequence, t: Throwable): Unit = { errorMessages += msg.toString; () }
  }

  test("forwards each level to the matching Maven log method when enabled") {
    val fake = new FakeMavenLog()
    val logger = new MavenMojoLogger(fake)

    logger.log(Level.Debug, "a debug")
    logger.log(Level.Info, "an info")
    logger.log(Level.Warn, "a warn")
    logger.log(Level.Error, "an error")

    assertEquals(fake.debugMessages.toList, List("a debug"))
    assertEquals(fake.infoMessages.toList, List("an info"))
    assertEquals(fake.warnMessages.toList, List("a warn"))
    assertEquals(fake.errorMessages.toList, List("an error"))
  }

  test("does not log debug when debug is disabled") {
    val fake = new FakeMavenLog(debugEnabled = false)
    new MavenMojoLogger(fake).log(Level.Debug, "a debug")
    assertEquals(fake.debugMessages.toList, List.empty)
  }

  test("does not log info when info is disabled") {
    val fake = new FakeMavenLog(infoEnabled = false)
    new MavenMojoLogger(fake).log(Level.Info, "an info")
    assertEquals(fake.infoMessages.toList, List.empty)
  }

  test("does not log warn when warn is disabled") {
    val fake = new FakeMavenLog(warnEnabled = false)
    new MavenMojoLogger(fake).log(Level.Warn, "a warn")
    assertEquals(fake.warnMessages.toList, List.empty)
  }

  test("does not log error when error is disabled") {
    val fake = new FakeMavenLog(errorEnabled = false)
    new MavenMojoLogger(fake).log(Level.Error, "an error")
    assertEquals(fake.errorMessages.toList, List.empty)
  }
}
