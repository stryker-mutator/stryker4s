package stryker4s.run
import stryker4s.testutil.Stryker4sSuite

class EtaCalculatorTest extends Stryker4sSuite{
  val oneMinute: Long = 60 * 1000

  describe("etacalculator"){
    it("should omit hours if time is below an hour"){
      val nrOfMutations = 61
      val etaCalculator = new EtaCalculator(nrOfMutations)
      etaCalculator.saveRunResult(oneMinute)
      etaCalculator.saveRunResult(oneMinute)

      etaCalculator.calculateETA(60) shouldEqual "1 hours 0 minutes and 0 seconds"
      etaCalculator.calculateETA(59) shouldEqual "59 minutes and 0 seconds"
    }

    it("should calculate the median correctly with an odd amount of runs") {
      val etaCalculator = new EtaCalculator(40)
      for (i <- 1 to 4) etaCalculator.saveRunResult(i * oneMinute)

      etaCalculator.saveRunResult(20 * oneMinute)
      val result = etaCalculator.calculateETA(1)

      result shouldEqual "3 minutes and 0 seconds" // median of 1-2-3-4-20 is 3
    }

    it("should calculate the median correctly with an even amount of runs"){
      val etaCalculator = new EtaCalculator(40)
      for(i <- 1 to 5) etaCalculator.saveRunResult(i * oneMinute)

      etaCalculator.saveRunResult(20 * oneMinute)
      val result = etaCalculator.calculateETA(1)

      result shouldEqual "3 minutes and 30 seconds" // median of 1-2-3-4-5-20 is 3.5
    }

    it("should base estimate only on the last 10 runs"){
      val nrOfMutations = 8
      val etaCalculator = new EtaCalculator(nrOfMutations)
      etaCalculator.saveRunResult(oneMinute * 1000)
      for(_ <- 1 to 11) etaCalculator.saveRunResult(oneMinute)

      etaCalculator.calculateETA(1) shouldEqual "1 minutes and 0 seconds"
    }
  }
}
