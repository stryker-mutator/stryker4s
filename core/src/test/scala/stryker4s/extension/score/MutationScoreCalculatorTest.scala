package stryker4s.extension.score

import stryker4s.testutil.Stryker4sSuite

class MutationScoreCalculatorTest extends Stryker4sSuite {
  case object Sut extends MutationScoreCalculator

  private val sut = Sut

  describe("Calculating the mutation score") {
    it("Should give a mutation score of 0.00 if no mutants were found") {
      val mutationScore = sut.calculateMutationScore(0, 0)

      mutationScore shouldBe 0.00
    }

    it("Should give a mutation score of 0.00 if 1 mutant is found and non are detected") {
      val mutationScore = sut.calculateMutationScore(1, 0)

      mutationScore shouldBe 0.00
    }

    it("Should give a mutation score of 100.00 when all mutations are killed") {
      val mutationScore = sut.calculateMutationScore(100, 100)

      mutationScore shouldBe 100.00
    }

    it("Should give a mutation score of 85.71 when 12 of the 14 mutations are killed") {
      val mutationScore = sut.calculateMutationScore(14, 12)

      mutationScore shouldBe 85.71
    }

    it("Should give a mutation score of 66.67 when 6 of the 9 mutations are killed") {
      val mutationScore = sut.calculateMutationScore(9, 6)

      mutationScore shouldBe 66.67
    }
  }
}
