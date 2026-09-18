package stryker4s.mutatorapi

import mutationtesting.{Location, Position as MTPosition}
import stryker4s.testkit.Stryker4sSuite

class MutantMetadataTest extends Stryker4sSuite {
  test("apply(Position) should convert a scalameta Position to a Location") {
    val tree = "x > 5".parseTerm
    val position = tree.pos

    val metadata = MutantMetadata("x > 5", "x <= 5", "GreaterThan", position, None)

    val expectedLocation = Location(
      start = MTPosition(line = position.startLine + 1, column = position.startColumn + 1),
      end = MTPosition(line = position.endLine + 1, column = position.endColumn + 1)
    )
    assertEquals(metadata.location, expectedLocation)
    assertEquals(metadata.original, "x > 5")
    assertEquals(metadata.replacement, "x <= 5")
    assertEquals(metadata.mutatorName, "GreaterThan")
    assertEquals(metadata.description, None)
  }

  test("apply(Location) should keep the given Location as-is") {
    val location = Location(MTPosition(1, 1), MTPosition(1, 5))

    val metadata = MutantMetadata("a", "b", "SomeMutator", location, Some("a description"))

    assertEquals(metadata.location, location)
    assertEquals(metadata.description, Some("a description"))
  }
}
