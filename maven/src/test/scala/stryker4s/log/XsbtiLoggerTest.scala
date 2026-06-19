package stryker4s.log

import stryker4s.testkit.{LogMatchers, Stryker4sSuite}

import java.util.function.Supplier

class XsbtiLoggerTest extends Stryker4sSuite with LogMatchers {

  private def supplier(s: String): Supplier[String] = () => s

  test("error is forwarded as an error log") {
    new XsbtiLogger().error(supplier("an error"))
    assertLoggedError("an error")
  }

  test("warn is forwarded as a debug log") {
    new XsbtiLogger().warn(supplier("a warn"))
    assertLoggedDebug("a warn")
    assertNotLoggedWarn("a warn")
  }

  test("info is forwarded as a debug log") {
    new XsbtiLogger().info(supplier("an info"))
    assertLoggedDebug("an info")
    assertNotLoggedInfo("an info")
  }

  test("debug is forwarded as a debug log") {
    new XsbtiLogger().debug(supplier("a debug"))
    assertLoggedDebug("a debug")
  }

  test("trace is forwarded as a debug log with a 'trace' message and the exception") {
    new XsbtiLogger().trace(() => new RuntimeException("boom"))
    assertLoggedDebug("trace")
    assertLoggedDebug("boom")
  }
}
