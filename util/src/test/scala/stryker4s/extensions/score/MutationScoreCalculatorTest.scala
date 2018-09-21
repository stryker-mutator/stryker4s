package stryker4s.extensions.score
import stryker4s.Stryker4sSuite

class MutationScoreCalculatorTest extends Stryker4sSuite {

  case object Sut extends MutationScoreCalculator

  private val sut = Sut

  describe("Calculating the mutation score") {
    it("Should give a mutation score of 0.0 if no mutants were found") {
      val mutationScore = sut.calculateMutationScore(0, 0)

      mutationScore shouldBe 0.00
    }

    it("Should give a mutation score of 100.0 when all mutations are killed") {
      val mutationScore = sut.calculateMutationScore(100, 100)

      mutationScore shouldBe 100.00
    }

    it("Should give a mutation score of 85.71 when 12 of the 14 mutations are killed") {
      val mutationScore = sut.calculateMutationScore(14, 12)

      mutationScore shouldBe 85.71
    }
  }
}
