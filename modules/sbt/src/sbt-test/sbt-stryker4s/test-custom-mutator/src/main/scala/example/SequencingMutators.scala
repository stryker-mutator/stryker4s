package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: swaps cats-effect sequencing operators `*>` and `<*`.
  *
  * Both effects still run, but the returned value changes from the right side to the left side (or vice versa). This
  * tests whether suites assert the result of sequencing, not only that it completed.
  */
class SequencingSwapMutator extends CustomMutator {
  def matcher: MutationMatcher = {
    case term @ Term.ApplyInfix.After_4_6_0(_, Term.Name(op), Type.ArgClause(Nil), args)
        if isSequencingOperator(op) && hasOneArgument(args) =>
      val replacement = if (op == "*>") "<*" else "*>"
      mutate(term, term.copyWithComments(op = term.op.copyWithComments(value = replacement)), replacement)
  }

  private def isSequencingOperator(op: String): Boolean =
    op == "*>" || op == "<*"

  private def hasOneArgument(args: Term.ArgClause): Boolean =
    args.values.size == 1

  private def mutate(term: Term.ApplyInfix, replacement: Term, to: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val metadata = MutantMetadata(term.op.value, to, "SequencingSwap", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(replacement, metadata)))
  }
}

/** Example custom mutator: removes the effect whose result is discarded by cats-effect sequencing.
  *
  * `fa *> fb` becomes `fb`, removing the left effect. `fa <* fb` becomes `fa`, removing the right effect. This keeps
  * the original result type while testing whether discarded-result effects are nevertheless behaviorally important.
  */
class SequencingRemovalMutator extends CustomMutator {
  def matcher: MutationMatcher = {
    case term @ Term.ApplyInfix.After_4_6_0(left, Term.Name("*>"), Type.ArgClause(Nil), args)
        if args.values.size == 1 =>
      mutate(term, args.values.head, "*>", "right")
    case term @ Term.ApplyInfix.After_4_6_0(left, Term.Name("<*"), Type.ArgClause(Nil), args)
        if args.values.size == 1 =>
      mutate(term, left, "<*", "left")
  }

  private def mutate(term: Term.ApplyInfix, replacement: Term, from: String, to: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val metadata = MutantMetadata(from, to, "SequencingRemoval", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(replacement, metadata)))
  }
}
