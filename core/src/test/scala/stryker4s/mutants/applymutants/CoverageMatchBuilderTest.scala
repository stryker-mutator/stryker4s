package stryker4s.mutants.applymutants

import stryker4s.extension.TreeExtensions.IsEqualExtension
import stryker4s.extension.mutationtype.GreaterThan
import stryker4s.model.{Mutant, MutantId, TransformedMutants}
import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sSuite

import scala.meta.*

class CoverageMatchBuilderTest extends Stryker4sSuite with LogMatchers {
  describe("buildMatch") {
    it("should add coverage analysis to the default case") {
      // Arrange
      val ids = Iterator.from(0)
      val originalStatement = q"x >= 15"
      val mutants = List(q"x > 15", q"x <= 15")
        .map(Mutant(MutantId(ids.next()), originalStatement, _, GreaterThan))
      val sut = new CoverageMatchBuilder(ActiveMutationContext.testRunner)

      // Act
      val result = sut.buildMatch(TransformedMutants(originalStatement, mutants)).cases.last

      // Assert
      assert(result.isEqual(p"case _ if _root_.stryker4s.coverage.coverMutant(0, 1) => x >= 15"), result)
    }

    it("should set the mutation switch match to Ints") {
      // Arrange
      val ids = Iterator.from(0)
      val originalStatement = q"x >= 15"
      val mutants = List(q"x > 15", q"x <= 15")
        .map(Mutant(MutantId(ids.next()), originalStatement, _, GreaterThan))
      val sut = new CoverageMatchBuilder(ActiveMutationContext.testRunner)

      // Act
      val result = sut.buildMatch(TransformedMutants(originalStatement, mutants)).cases.init

      // Assert
      result.map(_.syntax) should contain
        .inOrderOnly(
          p"case 0 => x > 15".syntax,
          p"case 1 => x <= 15".syntax
        )
    }
  }
}
