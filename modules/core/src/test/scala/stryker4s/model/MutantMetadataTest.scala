package stryker4s.model

import cats.syntax.show.*
import mutationtesting.{Location, Position}
import stryker4s.testkit.Stryker4sSuite

import MutantMetadata.locationShow

class MutantMetadataTest extends Stryker4sSuite {
  describe("locationShow") {
    test("should show the location") {

      val location = Location(Position(1, 2), Position(3, 4))
      assertEquals(location.show, "1:2 to 3:4")
    }

    test("should be equal to showLocation") {
      val location = Location(Position(1, 2), Position(3, 4))
      val metadata = MutantMetadata("foo", "bar", "baz", location)

      assertEquals(location.show, metadata.showLocation)
    }
  }
}
