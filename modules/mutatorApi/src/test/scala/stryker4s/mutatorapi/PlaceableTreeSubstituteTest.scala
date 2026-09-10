package stryker4s.mutatorapi

import stryker4s.testkit.Stryker4sSuite

import scala.meta.*

class PlaceableTreeSubstituteTest extends Stryker4sSuite {

  /** Re-parsing a snippet loses the position it had inside the enclosing tree, so tests that need a positioned sub-term
    * look it up in the real tree by syntax instead of parsing it separately.
    */
  private def subTerm(within: Tree, syntax: String): Term =
    within.collect { case sub: Term if sub.syntax == syntax => sub }.head

  test("substitute should replace the whole tree when it is itself the original") {
    val tree = "a.orElse(b)".parseTerm

    val result = PlaceableTree(tree).substitute(tree, "a".parseTerm)

    assertEquals(result.syntax, "a")
  }

  test("substitute should keep the surrounding context of a nested original") {
    val tree = "IO.sleep(d).as(q).timeout(l)".parseTerm
    val original = subTerm(tree, "IO.sleep(d)")

    val result = PlaceableTree(tree).substitute(original, "IO.unit".parseTerm)

    assertEquals(result.syntax, "IO.unit.as(q).timeout(l)")
  }

  test("substitute should replace a deeply nested original") {
    val tree = "xs.map(x => f(x) + 1)".parseTerm
    val original = subTerm(tree, "f(x)")

    val result = PlaceableTree(tree).substitute(original, "g(x)".parseTerm)

    assertEquals(result.syntax, "xs.map(x => g(x) + 1)")
  }

  test("substitute should replace only the occurrence at the original's position") {
    val tree = "f(a) + f(a)".parseTerm
    val original = tree.collect { case t: Term.Apply => t }.head

    val result = PlaceableTree(tree).substitute(original, "b".parseTerm)

    assertEquals(result.syntax, "b + f(a)")
  }

  test("substitute should find the original by reference identity") {
    val tree = "wrap(inner)".parseTerm
    val original = tree.collect { case t @ Term.Name("inner") => t }.head

    val result = PlaceableTree(tree).substitute(original, "replaced".parseTerm)

    assertEquals(result.syntax, "wrap(replaced)")
  }

  test("substitute should fail with a helpful message when the original is not in the tree") {
    val tree = "a.orElse(b)".parseTerm

    val thrown = intercept[IllegalArgumentException] {
      PlaceableTree(tree).substitute("somethingElse".parseTerm, "a".parseTerm)
    }

    assert(thrown.getMessage.contains("somethingElse"), thrown.getMessage)
    assert(thrown.getMessage.contains("a.orElse(b)"), thrown.getMessage)
  }
}
