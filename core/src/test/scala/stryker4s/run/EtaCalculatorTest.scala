package stryker4s.run
import stryker4s.testutil.Stryker4sSuite

class EtaCalculatorTest extends Stryker4sSuite{
  val oneMinute: Int = 60 * 1000

  describe("etacalculator"){
    it("should omit hours if time is below an hour"){
      val nrOfMutations = 61
      val etaCalculator = new EtaCalculator(nrOfMutations)
      etaCalculator.runResults(0) = oneMinute
      etaCalculator.runResults(1) = oneMinute
      etaCalculator.calculateETA(1) shouldEqual "1 hours 0 minutes and 0 seconds"
      etaCalculator.calculateETA(2) shouldEqual "59 minutes and 0 seconds"
    }

    it("should base estimate only on the last 5 runs"){
      val nrOfMutations = 8
      val etaCalculator = new EtaCalculator(nrOfMutations)
      etaCalculator.runResults(0) = oneMinute * 1000
      for(i <- 1 to 6) etaCalculator.runResults(i) = oneMinute

      etaCalculator.calculateETA(7) shouldEqual "1 minutes and 0 seconds"
    }
  }
}
