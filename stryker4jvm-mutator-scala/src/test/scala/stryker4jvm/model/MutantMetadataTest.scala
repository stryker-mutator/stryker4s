package stryker4jvm.model

import cats.syntax.show.*
import mutationtesting.{Location, Position}
import stryker4jvm.testutil.Stryker4sSuite

import MutantMetadata.locationShow

class MutantMetadataTest extends Stryker4sSuite {
  describe("locationShow") {
    it("should show the location") {

      val location = Location(Position(1, 2), Position(3, 4))
      location.show shouldBe "1:2 to 3:4"
    }

    it("should be equal to showLocation") {
      val location = Location(Position(1, 2), Position(3, 4))
      val metadata = MutantMetadata("foo", "bar", "baz", location)

      location.show shouldBe metadata.showLocation
    }
  }
}
