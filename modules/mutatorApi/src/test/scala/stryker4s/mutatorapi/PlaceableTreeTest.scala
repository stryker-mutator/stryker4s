package stryker4s.mutatorapi

import stryker4s.testkit.Stryker4sSuite

class PlaceableTreeTest extends Stryker4sSuite {
  test("toString should delegate to the wrapped tree's toString") {
    val tree = "x > 5".parseTerm
    val placeableTree = PlaceableTree(tree)

    assertEquals(placeableTree.toString(), tree.toString())
  }
}
