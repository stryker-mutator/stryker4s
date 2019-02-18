package stryker4s.run.report
import java.nio.file.Paths

import stryker4s.config.Config
import stryker4s.extension.mutationtype.EmptyString
import stryker4s.model.{Killed, Mutant, Survived}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

import scala.meta._

class ConsoleReporterTest extends Stryker4sSuite with LogMatchers {
  describe("start test run") {
    it("Should log that test run 1 is started when mutant id is 0") {
      implicit val config: Config = Config()
      val sut = new ConsoleReporter()
      val mutant = Mutant(0, q"4", q"5", EmptyString)

      sut.reportStartRun(mutant)

      "Starting test-run 1..." shouldBe loggedAsInfo
    }

    it("should log multiple test runs") {
      implicit val config: Config = Config()
      val sut = new ConsoleReporter()
      val mutant1 = Mutant(0, q"4", q"5", EmptyString)
      val mutant2 = Mutant(1, q"0", q"1", EmptyString)

      sut.reportStartRun(mutant1)
      sut.reportStartRun(mutant2)

      "Starting test-run 1..." shouldBe loggedAsInfo
      "Starting test-run 2..." shouldBe loggedAsInfo
    }

  }

  describe("finish mutation run") {

    it("Should log multiple test runs") {
      implicit val config: Config = Config()
      val sut = new ConsoleReporter()
      val mutant1 = Killed(Mutant(0, q"4", q"5", EmptyString), Paths.get("stryker4s"))
      val mutant2 = Survived(Mutant(1, q"0", q"1", EmptyString), Paths.get("stryker4s"))

      sut.reportFinishedMutation(mutant1, 2)
      sut.reportFinishedMutation(mutant2, 2)

      "Finished mutation run 1/2 (50%)" shouldBe loggedAsInfo
      "Finished mutation run 2/2 (100%)" shouldBe loggedAsInfo
    }
  }

}
