package stryker4s.testkit

import fansi.Attr
import fansi.Color.*
import munit.{BaseFunSuite, Location, Suite, TestTransforms}
import stryker4s.log.Level

protected[stryker4s] trait LogMatchers extends Suite with TestTransforms {
  this: BaseFunSuite =>

  /** Override to `true` if you want tests to print logs to the console for debugging purposes.
    *
    * Tests with errors will always print logs (afterwards).
    *
    * @example
    *   `override def printLogs = true`
    */
  def printLogs: Boolean = false

  implicit lazy val testLogger: TestLogger = testLoggerFixture()

  private val testLoggerFixture = new Fixture[TestLogger]("testlogger") {
    val testLogger: TestLogger = new TestLogger(printLogs)
    override def apply(): TestLogger = testLogger
    override def afterEach(context: AfterEach): Unit = testLogger.clear()
  }

  override def munitFixtures = super.munitFixtures ++ List(testLoggerFixture)

  override def munitTestTransforms: List[TestTransform] = super.munitTestTransforms ++ List(
    new TestTransform(
      "print logs on failure",
      { test =>
        test.withBodyMap { body =>
          body.recover {
            case exception if !printLogs => testLogger.printAllLogs(); throw exception

          }(munitExecutionContext)
        }
      }
    )
  )

  def assertLoggedDebug(msg: String)(implicit loc: Location): Unit = findForLevel(Level.Debug, msg).assertSuccess()
  def assertLoggedInfo(msg: String)(implicit loc: Location): Unit = findForLevel(Level.Info, msg).assertSuccess()
  def assertLoggedWarn(msg: String)(implicit loc: Location): Unit = findForLevel(Level.Warn, msg).assertSuccess()
  def assertLoggedError(msg: String)(implicit loc: Location): Unit = findForLevel(Level.Error, msg).assertSuccess()

  def assertNotLoggedDebug(msg: String)(implicit loc: Location): Unit = findForLevel(Level.Debug, msg).assertFailure()
  def assertNotLoggedInfo(msg: String)(implicit loc: Location): Unit = findForLevel(Level.Info, msg).assertFailure()
  def assertNotLoggedWarn(msg: String)(implicit loc: Location): Unit = findForLevel(Level.Warn, msg).assertFailure()
  def assertNotLoggedError(msg: String)(implicit loc: Location): Unit = findForLevel(Level.Error, msg).assertFailure()

  private def findForLevel(expectedLogLevel: Level, expectedLogMessage: String) = {
    testLogger.findEvent(expectedLogMessage) match {
      case None =>
        testLogger.findEventPlainText(expectedLogMessage) match {
          case Some((_, message)) =>
            val msg =
              s"""Message was logged with level ${expectedLogLevel.toString().toLowerCase()}, but with different colors.
                 |${Red("Obtained:")}
                 |${Attr.Reset.escape}$message
                 |
                 |${Green("Expected:")}
                 |${Attr.Reset.escape}$expectedLogMessage
                 |
                 |""".stripMargin
            MatchResult(matches = false, msg, msg)
          case None =>
            MatchResult(
              matches = false,
              s"Message '$expectedLogMessage' wasn't logged at any level.",
              s"Message '$expectedLogMessage' was logged as ${expectedLogLevel.toString().toLowerCase()}."
            )
        }
      case Some((level, _)) =>
        MatchResult(
          level == expectedLogLevel,
          s"Message '$expectedLogMessage' was logged, but not on correct log level. Expected [${Green(
              expectedLogLevel.toString().toLowerCase()
            )}] actual [${Red(level.toString().toLowerCase())}].",
          s"Message '$expectedLogMessage' was logged as ${expectedLogLevel.toString().toLowerCase()}."
        )
    }
  }

  /** Similar to
    * https://github.com/scalatest/scalatest/blob/main/jvm/matchers-core/src/main/scala/org/scalatest/matchers/MatchResult.scala
    */
  protected[testkit] case class MatchResult(matches: Boolean, failureMessage: String, negatedFailureMessage: String) {
    def assertSuccess()(implicit loc: Location): Unit = {
      assert(matches, failureMessage)
    }
    def assertFailure()(implicit loc: Location): Unit = {
      assert(!matches, negatedFailureMessage)
    }
  }
}
