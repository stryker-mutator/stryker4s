package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: removes deliberate delays.
  *
  * `IO.sleep(backoff)` becomes `IO.unit`, and `publish.delayBy(backoff)` / `publish.andWait(backoff)` become `publish`.
  * The surrounding effect keeps its type and still runs; only the waiting is gone.
  *
  * This separates load-bearing delays from cargo-culted ones. A sleep that is genuinely part of the behaviour — a
  * backoff between retries, a rate limit, a debounce window — should have a test that fails without it. One that
  * survives is either untested or was never needed.
  *
  * The qualifier is preserved rather than hardcoded to `IO`, so `Temporal[F].sleep(d)` correctly becomes
  * `Temporal[F].unit`; `unit` is available on any `Applicative`. `Thread.sleep` is excluded because `Thread` has no
  * `unit` and the mutant could only ever be a compile error.
  */
class SleepRemovalMutator extends CustomMutator {
  def matcher: MutationMatcher = {
    case term @ Term.Apply.After_4_6_0(Term.Select(qualifier, Term.Name("sleep")), args)
        if args.values.size == 1 && !isThread(qualifier) =>
      mutate(term, Term.Select(qualifier, Term.Name("unit")), "sleep")

    case term @ Term.Apply.After_4_6_0(Term.Select(receiver, Term.Name(method)), args)
        if isDelay(method) && args.values.size == 1 =>
      mutate(term, receiver, method)
  }

  private def isDelay(name: String): Boolean =
    name == "delayBy" || name == "andWait"

  private def isThread(qualifier: Term): Boolean =
    qualifier match {
      case Term.Name("Thread")                 => true
      case Term.Select(_, Term.Name("Thread")) => true
      case _                                   => false
    }

  private def mutate(term: Term, replacement: Term, removed: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val metadata = MutantMetadata(removed, "no delay", "SleepRemoval", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(term, replacement), metadata)))
  }
}
