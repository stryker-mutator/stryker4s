package mill.api.daemon {

  import java.io.{ByteArrayInputStream, PrintStream}
  import scala.collection.mutable.ListBuffer

  /** A test double for Mill's [[Logger]]. Lives in `mill.api.daemon` because `prompt` (which backs `debugEnabled` and
    * color detection) is `private[mill]` and can only be overridden from within the `mill` package.
    */
  class FakeMillLogger(debugOn: Boolean, coloredOn: Boolean) extends Logger {
    val logged: ListBuffer[(String, String)] = ListBuffer.empty

    override val streams: SystemStreams = new SystemStreams(
      new PrintStream(_ => ()),
      new PrintStream(_ => ()),
      new ByteArrayInputStream(Array.empty)
    )

    override def info(s: String): Unit = logged += (("info", s))
    override def debug(s: String): Unit = logged += (("debug", s))
    override def warn(s: String): Unit = logged += (("warn", s))
    override def error(s: String): Unit = logged += (("error", s))
    override def ticker(s: String): Unit = logged += (("ticker", s))

    override private[mill] def prompt: Logger.Prompt = new Logger.Prompt.NoOp {
      override def debugEnabled: Boolean = debugOn
      override def colored: Boolean = coloredOn
    }
  }
}

package stryker4s.log {

  import mill.api.daemon.FakeMillLogger
  import stryker4s.testkit.Stryker4sSuite

  class MillLoggerTest extends Stryker4sSuite {

    /** Exposes the `protected` `colorEnabled` for assertions. */
    private class ProbeLogger(colored: Boolean, env: Map[String, String])
        extends MillLogger(new FakeMillLogger(debugOn = false, coloredOn = colored), env) {
      def color: Boolean = colorEnabled
    }

    private def colorOf(colored: Boolean, env: Map[String, String]): Boolean =
      new ProbeLogger(colored, env).color

    test("debug messages are only logged when mill has debug enabled") {
      val enabled = new FakeMillLogger(debugOn = true, coloredOn = false)
      new MillLogger(enabled, Map.empty).log(Level.Debug, "debug message")
      assertEquals(enabled.logged.toList, List("debug" -> "debug message"))

      val disabled = new FakeMillLogger(debugOn = false, coloredOn = false)
      new MillLogger(disabled, Map.empty).log(Level.Debug, "debug message")
      assertEquals(disabled.logged.toList, List.empty)
    }

    test("info, warn and error are always forwarded to mill") {
      val millLogger = new FakeMillLogger(debugOn = false, coloredOn = false)
      val logger = new MillLogger(millLogger, Map.empty)
      logger.log(Level.Info, "an info")
      logger.log(Level.Warn, "a warning")
      logger.log(Level.Error, "an error")
      assertEquals(
        millLogger.logged.toList,
        List("info" -> "an info", "warn" -> "a warning", "error" -> "an error")
      )
    }

    test("color is enabled when mill enables color and NO_COLOR is unset") {
      assert(colorOf(colored = true, env = Map.empty))
    }

    test("color is disabled when NO_COLOR is set, even if mill enables color") {
      assert(!colorOf(colored = true, env = Map("NO_COLOR" -> "")))
    }

    test("color is disabled when mill disables color, even if NO_COLOR is unset") {
      assert(!colorOf(colored = false, env = Map.empty))
    }
  }
}
