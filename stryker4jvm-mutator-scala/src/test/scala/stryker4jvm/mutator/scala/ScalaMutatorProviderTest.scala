package stryker4jvm.mutator.scala

import org.scalatest.BeforeAndAfterEach
import stryker4jvm.mutator.scala.ScalaMutatorProvider.parseDialect
import stryker4jvm.mutator.scala.testutil.{Stryker4jvmSuite, TestLogger}

import scala.meta.dialects.*

class ScalaMutatorProviderTest extends Stryker4jvmSuite with BeforeAndAfterEach {
  val logger = new TestLogger()

  override def afterEach(): Unit = {
    logger.clear()
  }

  describe("parseDialect") {
    val validVersions = Map(
      "scala212" -> Scala212,
      "scala2.12" -> Scala212,
      "2.12" -> Scala212,
      "212" -> Scala212,
      "scala212source3" -> Scala212Source3,
      "scala213" -> Scala213,
      "scala2.13" -> Scala213,
      "2.13" -> Scala213,
      "213" -> Scala213,
      "2" -> Scala213,
      "scala213source3" -> Scala213Source3,
      "source3" -> Scala213Source3,
      "scala3future" -> Scala3Future,
      "future" -> Scala3Future,
      "scala30" -> Scala30,
      "scala3.0" -> Scala30,
      "3.0" -> Scala30,
      "30" -> Scala30,
      "dotty" -> Scala30,
      "scala31" -> Scala31,
      "scala3.1" -> Scala31,
      "3.1" -> Scala31,
      "31" -> Scala31,
      "scala32" -> Scala32,
      "scala3.2" -> Scala32,
      "3.2" -> Scala32,
      "32" -> Scala32,
      "scala3" -> Scala3,
      "scala3.0" -> Scala3,
      "3.0" -> Scala3,
      "3" -> Scala3
    )

    validVersions.foreach { case (input, expected) =>
      it(s"should parse $input to $expected") {
        parseDialect(input, logger) shouldBe expected
        logger.logs.isEmpty shouldBe true
      }
    }

    it("should not parse invalid scala-dialects") {
      val invalidDialect = "Invalid Dialect"
      val dialect = parseDialect(invalidDialect, logger)
      logger.logs.isEmpty shouldBe false
      logger.logs.head.contains("Unknown") shouldBe true
      dialect shouldBe Scala213
    }

    val deprecatedVersions = List("scala211", "scala2.11", "2.11", "211")

    deprecatedVersions.foreach { version =>
      it(s"should error deprecated scala-dialect $version") {
        val dialect = parseDialect(version, logger)
        logger.logs.isEmpty shouldBe false
        logger.logs.head.contains("deprecated") shouldBe true
        dialect shouldBe Scala211
      }
    }
  }
}
