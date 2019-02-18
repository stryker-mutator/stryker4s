package stryker4s.run.report
import java.io.File
import java.nio.file.Paths

import stryker4s.config.{Config, Thresholds}
import stryker4s.model
import stryker4s.model.{Killed, Mutant, Survived}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

import scala.concurrent.duration._
import scala.meta._

class ConsoleReporterTest extends Stryker4sSuite with LogMatchers {
  describe("reportStartRun") {
    it("Should log that test run 1 is started when mutant id is 0") {
      implicit val config: Config = Config()
      val sut = new ConsoleReporter()
      val mutant = Mutant(0, q"4", q"5")

      sut.reportStartRun(mutant)

      "Starting test-run 1..." shouldBe loggedAsInfo
    }

    it("should log multiple test runs") {
      implicit val config: Config = Config()
      val sut = new ConsoleReporter()
      val mutant1 = Mutant(0, q"4", q"5")
      val mutant2 = Mutant(1, q"0", q"1")

      sut.reportStartRun(mutant1)
      sut.reportStartRun(mutant2)

      "Starting test-run 1..." shouldBe loggedAsInfo
      "Starting test-run 2..." shouldBe loggedAsInfo
    }

  }

  describe("reportFinishedMutation") {

    it("Should log multiple test runs") {
      implicit val config: Config = Config()
      val sut = new ConsoleReporter()
      val mutant1 = Killed(Mutant(0, q"4", q"5"), Paths.get("stryker4s"))
      val mutant2 = Survived(Mutant(1, q"0", q"1"), Paths.get("stryker4s"))

      sut.reportFinishedMutation(mutant1, 2)
      sut.reportFinishedMutation(mutant2, 2)

      "Finished mutation run 1/2 (50%)" shouldBe loggedAsInfo
      "Finished mutation run 2/2 (100%)" shouldBe loggedAsInfo
    }
  }

  describe("reportFinishedRun") {
    val pathSeparator = File.separator
    it("should report a finished run with multiple mutants") {
      implicit val config: Config = Config()
      val sut = new ConsoleReporter()
      val results = model.MutantRunResults(
        Seq(
          Killed(Mutant(0, q"4", q"5"), Paths.get("stryker4s")),
          Survived(Mutant(1, q"0", q"1"), Paths.get("stryker4s")),
          Survived(Mutant(2, q"1", q"2"), Paths.get("stryker4s/subPath"))
        ),
        50,
        15.seconds
      )
      sut.reportFinishedRun(results)

      "Mutation run finished! Took 15 seconds" shouldBe loggedAsInfo
      "Total mutants: 3, detected: 1, undetected: 2" shouldBe loggedAsInfo
      s"""Undetected mutants:
         |1. [Survived]
         |stryker4s:0:0
         |-	0
         |+	1
         |
         |2. [Survived]
         |stryker4s${pathSeparator}subPath:0:0
         |-	1
         |+	2
         |""".stripMargin shouldBe loggedAsInfo
    }

    it("should log mutants sorted by id") {
      implicit val config: Config = Config()
      val sut = new ConsoleReporter()
      val results = model.MutantRunResults(
        Seq(
          Survived(Mutant(0, q"4", q"5"), Paths.get("stryker4s")),
          Survived(Mutant(2, q"1", q"2"), Paths.get("stryker4s/subPath")),
          Survived(Mutant(1, q"0", q"1"), Paths.get("stryker4s"))
        ),
        50,
        15.seconds
      )
      sut.reportFinishedRun(results)

      "Mutation run finished! Took 15 seconds" shouldBe loggedAsInfo
      "Total mutants: 3, detected: 1, undetected: 2" shouldBe loggedAsInfo
      s"""Undetected mutants:
         |0. [Survived]
         |stryker4s:0:0
         |-	4
         |+	5
         |
         |1. [Survived]
         |stryker4s:0:0
         |-	0
         |+	1
         |
         |2. [Survived]
         |stryker4s${pathSeparator}subPath:0:0
         |-	1
         |+	2
         |""".stripMargin shouldBe loggedAsInfo
    }

    it("should report the mutation score when it is dangerously low") {
      implicit val config: Config = Config()
      val sut = new ConsoleReporter()
      val results = model.MutantRunResults(
        Seq(),
        50,
        15.seconds
      )
      sut.reportFinishedRun(results)

      "Mutation score dangerously low!" shouldBe loggedAsError
      "Mutation score: 50.0%" shouldBe loggedAsError
    }

    it("should report the mutation score when it is warning") {
      implicit val config: Config = Config(thresholds = Thresholds(break = 49, low = 50, high = 51))
      val sut = new ConsoleReporter()
      val results = model.MutantRunResults(
        Seq(),
        50,
        15.seconds
      )
      sut.reportFinishedRun(results)

      "Mutation score: 50.0%" shouldBe loggedAsWarning
    }

    it("should report the mutation score when it is info") {
      implicit val config: Config = Config(thresholds = Thresholds(break = 48, low = 49, high = 50))
      val sut = new ConsoleReporter()
      val results = model.MutantRunResults(
        Seq(),
        50,
        15.seconds
      )
      sut.reportFinishedRun(results)

      "Mutation score: 50.0%" shouldBe loggedAsInfo
    }

    it("should log when below threshold") {
      implicit val config: Config = Config(thresholds = Thresholds(break = 51, low = 52, high = 53))
      val sut = new ConsoleReporter()
      val results = model.MutantRunResults(
        Seq(),
        50,
        15.seconds
      )
      sut.reportFinishedRun(results)

      "Mutation score below threshold! Score: 50.0%. Threshold: 51%" shouldBe loggedAsError
    }
  }

}
