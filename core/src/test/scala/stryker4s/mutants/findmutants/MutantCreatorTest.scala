package stryker4s.mutants.findmutants

import stryker4s.Stryker4sSuite
import stryker4s.extensions.mutationtypes.{EqualTo, GreaterThan, GreaterThanEqualTo, LesserThan}
import stryker4s.extensions.ImplicitMutationConversion.mutationToTree

class MutantCreatorTest extends Stryker4sSuite {

  case class Sut() extends MutantCreator

  describe("mutant id") {
    it("should register multiple mutants from a FoundMutant with multiple mutations") {
      val sut = Sut()
      val mutants = sut.create(GreaterThan, LesserThan, GreaterThanEqualTo, EqualTo)

      mutants.map(mutant => mutant.id) should contain theSameElementsAs List(0,1,2)
    }
  }
}
