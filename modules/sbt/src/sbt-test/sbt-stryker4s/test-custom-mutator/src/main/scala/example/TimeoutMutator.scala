package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: removes cats-effect timeout combinators.
  *
  * `fa.timeout(d)`, `fa.timeoutTo(d, fallback)` and `fa.timeoutAndForget(d)` all become `fa`, so the effect is allowed
  * to run to completion instead of being cancelled at the deadline. This tests whether a suite actually asserts the
  * timeout behaviour, rather than only exercising the happy path where the deadline is never reached.
  *
  * Note that a surviving mutant here means no test distinguishes "completed in time" from "cancelled at the deadline".
  * A killed mutant will usually fail on the result value; if a suite has no such assertion the mutant may instead be
  * detected by Stryker4s's own test-run timeout.
  */
class TimeoutRemovalMutator extends CustomMutator {
  def matcher: MutationMatcher = {
    case term @ Term.Apply.After_4_6_0(Term.Select(receiver, Term.Name(name)), args)
        if isTimeoutCombinator(name, args.values.size) =>
      mutate(term, receiver, name)
  }

  private def isTimeoutCombinator(name: String, arity: Int): Boolean =
    ((name == "timeout" || name == "timeoutAndForget") && arity == 1) ||
      (name == "timeoutTo" && arity == 2)

  private def mutate(term: Term.Apply, replacement: Term, removed: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val metadata = MutantMetadata(removed, "removed", "TimeoutRemoval", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(term, replacement), metadata)))
  }
}
