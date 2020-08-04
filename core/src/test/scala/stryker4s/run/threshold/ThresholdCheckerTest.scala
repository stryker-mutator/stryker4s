package stryker4s.run.threshold

import stryker4s.config.{Config, Thresholds}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

class ThresholdCheckerTest extends Stryker4sSuite with LogMatchers {
  describe("thresholdchecker") {
    it("should return a DangerStatus with default thresholds and score 0.0") {
      implicit val config: Config = Config.default

      val score = 0.0

      val exitCode = ThresholdChecker.determineScoreStatus(score)

      exitCode shouldBe DangerStatus
    }

    it("should return a SuccessStatus when the score is above threshold") {
      implicit val config: Config = Config(thresholds = Thresholds(break = 10))

      val score = 100.0

      val exitCode = ThresholdChecker.determineScoreStatus(score)

      exitCode shouldBe SuccessStatus
    }

    it("should return a DangerStatus when the score equals threshold") {
      val score = 20
      implicit val config: Config = Config(thresholds = Thresholds(break = score))

      val exitCode = ThresholdChecker.determineScoreStatus(score.toDouble)

      exitCode shouldBe DangerStatus
    }

    it("should return an ErrorStatus when the threshold is not met") {
      implicit val config: Config = Config(thresholds = Thresholds(break = 50))

      val score = 10.0

      val exitCode = ThresholdChecker.determineScoreStatus(score)

      exitCode shouldBe ErrorStatus
    }

    it("should never return WarningStatus when 'high' is equal to 'low'") {
      implicit val config: Config = Config(thresholds = Thresholds(high = 85, low = 85))

      val lowerScoreStatus = ThresholdChecker.determineScoreStatus(84)
      val equalScoreStatus = ThresholdChecker.determineScoreStatus(85)
      val higherScoreStatus = ThresholdChecker.determineScoreStatus(86)

      lowerScoreStatus shouldBe DangerStatus
      equalScoreStatus shouldBe SuccessStatus
      higherScoreStatus shouldBe SuccessStatus
    }

    it("should return success status when score is equal to or greater than 'high' threshold") {
      implicit val config: Config = Config(thresholds = Thresholds(high = 85))

      val equalScoreStatus = ThresholdChecker.determineScoreStatus(85)
      val higherScoreStatus = ThresholdChecker.determineScoreStatus(90)

      equalScoreStatus shouldBe SuccessStatus
      higherScoreStatus shouldBe SuccessStatus
    }

    it("should return warning status when score is below 'high' and above or equal to 'low' threshold") {
      implicit val config: Config = Config(thresholds = Thresholds(low = 70))

      val equalScoreStatus = ThresholdChecker.determineScoreStatus(70)
      val higherScoreStatus = ThresholdChecker.determineScoreStatus(75)

      equalScoreStatus shouldBe WarningStatus
      higherScoreStatus shouldBe WarningStatus
    }

    it("should return danger status when score is below 'low' and above or equal to 'break' threshold") {
      implicit val config: Config = Config(thresholds = Thresholds(low = 20, break = 10))

      val equalScoreStatus = ThresholdChecker.determineScoreStatus(10)
      val higherScoreStatus = ThresholdChecker.determineScoreStatus(15)

      equalScoreStatus shouldBe DangerStatus
      higherScoreStatus shouldBe DangerStatus
    }

    it("should return error status when score is below 'break'") {
      implicit val config: Config = Config(thresholds = Thresholds(break = 10))

      val lowerScoreStatus = ThresholdChecker.determineScoreStatus(9)

      lowerScoreStatus shouldBe ErrorStatus
    }
  }
}
