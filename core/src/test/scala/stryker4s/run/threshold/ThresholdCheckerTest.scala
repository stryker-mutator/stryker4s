package stryker4s.run.threshold

import stryker4s.Stryker4sSuite
import stryker4s.config.{Config, Thresholds}
import stryker4s.model.MutantRunResults
import stryker4s.scalatest.LogMatchers

import scala.concurrent.duration._
import scala.language.postfixOps

class ThresholdCheckerTest extends Stryker4sSuite with LogMatchers{

    describe("thresholdchecker") {
      it("should return exitcode 0 when no threshold is required") {
        implicit val config: Config = Config()

        val mutantRunResults = MutantRunResults(List.empty, 0.0, 10 seconds)

        val exitCode = ThresholdChecker.determineExitCode(mutantRunResults)

        exitCode shouldBe 0
      }

      it("should return exitcode 0 when the score is above threshold") {
        implicit val config: Config = Config(thresholds = Thresholds(break = 10))

        val mutantRunResults = MutantRunResults(List.empty, 100.0, 10 seconds)

        val exitCode = ThresholdChecker.determineExitCode(mutantRunResults)

        exitCode shouldBe 0
      }

      it("should return exitcode 0 when the score equals threshold") {
        val score = 20
        implicit val config: Config = Config(thresholds = Thresholds(break = score))

        val mutantRunResults = MutantRunResults(List.empty, score, 10 seconds)

        val exitCode = ThresholdChecker.determineExitCode(mutantRunResults)

        exitCode shouldBe 0
      }

      it("should return exitcode 1 when the threshold is not met") {
        implicit val config: Config = Config(thresholds = Thresholds(break = 50))

        val mutantRunResults = MutantRunResults(List.empty, 10.0, 10 seconds)

        val exitCode = ThresholdChecker.determineExitCode(mutantRunResults)

        exitCode shouldBe 1
      }

      it("should return success status when score is equal to or greater than 'high' threshold") {
        implicit val config: Config = Config(thresholds = Thresholds(high = 85))

        val equalScoreStatus = ThresholdChecker.getScoreStatus(85)
        val higherScoreStatus = ThresholdChecker.getScoreStatus(90)

        equalScoreStatus shouldBe SuccessStatus
        higherScoreStatus shouldBe SuccessStatus
      }

      it("should return warning status when score is below 'high' and above or equal to 'low' threshold") {
        implicit val config: Config = Config(thresholds = Thresholds(low = 70))

        val equalScoreStatus = ThresholdChecker.getScoreStatus(70)
        val higherScoreStatus = ThresholdChecker.getScoreStatus(75)

        equalScoreStatus shouldBe WarningStatus
        higherScoreStatus shouldBe WarningStatus
      }

      it("should return danger status when score is below 'low' and above or equal to 'break' threshold") {
        implicit val config: Config = Config(thresholds = Thresholds(low = 20, break = 10))

        val equalScoreStatus = ThresholdChecker.getScoreStatus(10)
        val higherScoreStatus = ThresholdChecker.getScoreStatus(15)

        equalScoreStatus shouldBe DangerStatus
        higherScoreStatus shouldBe DangerStatus
      }

      it("should return error status when score is below 'break'") {
        implicit val config: Config = Config(thresholds = Thresholds(break = 10))

        val lowerScoreStatus = ThresholdChecker.getScoreStatus(9)

        lowerScoreStatus shouldBe ErrorStatus
      }
    }
}
