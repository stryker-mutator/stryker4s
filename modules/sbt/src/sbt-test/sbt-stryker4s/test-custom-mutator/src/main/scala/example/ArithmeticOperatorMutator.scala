package example

import cats.data.NonEmptyVector
import cats.syntax.all.*
import stryker4s.plugin.api.*

import scala.meta.*

/** Example custom mutator: swaps `+` for `-` (and vice versa) in `Int` arithmetic expressions.
  *
  * This demonstrates the "Arithmetic Operator" mutator category that Stryker4s does not ship built-in, registered here
  * via the `CustomMutatorPlugin` service provider.
  */
class ArithmeticOperatorMutator extends CustomMutator {
  def matcher: MutationMatcher = {
    case term @ Term.ApplyInfix.After_4_6_0(_, Term.Name("+"), Type.ArgClause(Nil), _) =>
      mutate(term, "-")
    case term @ Term.ApplyInfix.After_4_6_0(_, Term.Name("-"), Type.ArgClause(Nil), _) =>
      mutate(term, "+")
  }

  private def mutate(term: Term.ApplyInfix, to: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val from = term.op.value
    val mutated = term.copyWithComments(op = term.op.copyWithComments(value = to))
    val metadata = MutantMetadata(from, to, "ArithmeticOperator", term.pos, None)
    placeableTree
      .substitute(term, mutated)
      .bimap(
        reason => NonEmptyVector.one(MutatedCode(term, metadata) -> reason),
        code => NonEmptyVector.one(MutatedCode(code, metadata))
      )
  }
}
