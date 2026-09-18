package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: increments integer literals by 1 (e.g. `10` -> `11`).
  *
  * Demonstrates the "Numeric Literal" mutator category that Stryker4s does not ship built-in, registered here via the
  * `strykerCustomMutators` sbt setting. Purely syntactic (operates on `Lit.Int` nodes), with no dependency on type
  * information — matching the same style as Stryker4s's built-in `StringLiteral`/`BooleanLiteral` mutators. Only
  * produces a single (+1) mutant per literal, since scalameta trees created reflectively across a classloader boundary
  * can't safely use varargs-based collection construction (e.g. `NonEmptyVector.of`, `Vector(...)`) — see the
  * classloader-identity notes in `docs/configuration.md`.
  */
class NumericLiteralMutator extends CustomMutator {
  def matcher: MutationMatcher = { case lit @ Lit.Int(value) =>
    mutate(lit, value + 1)
  }

  private def mutate(lit: Lit.Int, incremented: Int)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val from = lit.value.toString
    val metadata = MutantMetadata(from, incremented.toString, "NumericLiteral", lit.pos, None)
    Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(lit, Lit.Int(incremented)), metadata)))
  }
}
