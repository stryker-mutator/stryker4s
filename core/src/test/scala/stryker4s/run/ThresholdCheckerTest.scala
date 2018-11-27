package stryker4s.run

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

        val thresholdChecker = new ThresholdChecker()
        val mutantRunResults = MutantRunResults(List.empty, 100.0, 10 seconds)

        val exitCode = thresholdChecker.determineExitCode(mutantRunResults)

        exitCode shouldBe 0
        "Threshold not configured. Won\'t fail the build no matter how low your mutation score is." shouldBe loggedAsDebug
        "Set `thresholds.break` to change this behavior." shouldBe loggedAsDebug
      }

      it("should return exitcode 0 when the threshold is met") {
        implicit val config: Config = Config(thresholds = Some(Thresholds(break = 10)))

        val thresholdChecker = new ThresholdChecker()
        val mutantRunResults = MutantRunResults(List.empty, 100.0, 10 seconds)

        val exitCode = thresholdChecker.determineExitCode(mutantRunResults)

        exitCode shouldBe 0
        "Mutation score 100.0 was above or equal to the configured threshold." shouldBe loggedAsInfo
      }

      it("should return exitcode 0 when the score equals threshold") {
        val score = 20
        implicit val config: Config = Config(thresholds = Some(Thresholds(break = score)))

        val thresholdChecker = new ThresholdChecker()
        val mutantRunResults = MutantRunResults(List.empty, score, 10 seconds)

        val exitCode = thresholdChecker.determineExitCode(mutantRunResults)

        exitCode shouldBe 0
        "Mutation score 20.0 was above or equal to the configured threshold." shouldBe loggedAsInfo
      }

      it("should return exitcode 1 when the threshold is not met") {
        implicit val config: Config = Config(thresholds = Some(Thresholds(break = 50)))

        val thresholdChecker = new ThresholdChecker()
        val mutantRunResults = MutantRunResults(List.empty, 10.0, 10 seconds)

        val exitCode = thresholdChecker.determineExitCode(mutantRunResults)

        exitCode shouldBe 1
        "Mutation score below threshold! Score: 10.0. Threshold: 50" shouldBe loggedAsError
      }
    }
}
