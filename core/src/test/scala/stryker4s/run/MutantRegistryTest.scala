package stryker4s.run

import stryker4s.Stryker4sSuite
import stryker4s.extensions.ImplicitMutationConversion.mutationToTree
import stryker4s.extensions.mutationtypes.{EqualTo, GreaterThan, GreaterThanEqualTo, LesserThan}
import stryker4s.model.{FoundMutant, Mutant, RegisteredMutant}

class MutantRegistryTest extends Stryker4sSuite {
  describe("registerMutant") {
    it("should register the first mutant with id 0") {
      val sut = new MutantRegistry
      val mutant = FoundMutant(GreaterThan, LesserThan)

      val result = sut.registerMutant(mutant)

      result should equal(RegisteredMutant(GreaterThan, List(Mutant(0, GreaterThan, LesserThan))))
    }

    it("should register two mutants with id 0 and 1") {
      val sut = new MutantRegistry
      val mutant = FoundMutant(GreaterThan, LesserThan)
      val secondMutant = FoundMutant(LesserThan, GreaterThan)

      val result = sut.registerMutant(mutant).mutants.loneElement
      val scndResult = sut.registerMutant(secondMutant).mutants.loneElement

      result should equal(Mutant(0, GreaterThan, LesserThan))
      scndResult should equal(Mutant(1, LesserThan, GreaterThan))
    }

    it("should register multiple mutants from a FoundMutant with multiple mutations") {
      val sut = new MutantRegistry
      val mutant =
        FoundMutant(GreaterThan, LesserThan, GreaterThanEqualTo, EqualTo)

      val result = sut.registerMutant(mutant)

      val expectedMutants = List(Mutant(0, GreaterThan, LesserThan),
                                 Mutant(1, GreaterThan, GreaterThanEqualTo),
                                 Mutant(2, GreaterThan, EqualTo))
      result should equal(RegisteredMutant(GreaterThan, expectedMutants))
    }
  }
}
