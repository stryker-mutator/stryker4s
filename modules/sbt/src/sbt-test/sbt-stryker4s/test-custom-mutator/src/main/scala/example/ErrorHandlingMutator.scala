package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: drops cats-effect/cats `MonadThrow`/`ApplicativeError` error-handling combinators
  * (`.attempt`, `.handleErrorWith`, `.handleError`, `.recover`, `.recoverWith`, `.rethrow`), replacing
  * `effect.combinator(...)` (or bare `effect.rethrow`/`effect.attempt`) with just `effect`.
  *
  * Demonstrates a mutator category not covered by any of Stryker4s's built-in mutators nor by the generic
  * imperative-language categories (Arithmetic/Numeric/Collection Literal) added earlier: FP error-handling paths.
  * Dropping the combinator either removes error recovery entirely (so a failure that should have been recovered now
  * propagates) or removes error capture (so a failure that should have been turned into a value now propagates) — both
  * are realistic, high-value mutants that specifically test whether error-handling logic is exercised by tests.
  *
  * Purely syntactic: matches on the method name in a `Term.Select`/`Term.Apply` and produces a mutant that is just the
  * receiver expression, dropping the wrapper — the same shape as `CollectionLiteralMutator`, with no dependency on type
  * information (so it can't distinguish a real `IO`/`F[_]` receiver from an unrelated type that happens to define a
  * same-named method; acceptable here since these method names are effectively reserved vocabulary in the
  * cats/cats-effect ecosystem).
  *
  * The applied combinators (`handleErrorWith` and friends) and the no-argument ones (`attempt`, `rethrow`) are kept in
  * disjoint sets on purpose. Stryker4s walks every node of the tree, so matching both `effect.recover(f)` and its inner
  * `effect.recover` selection would emit two mutants for one combinator — and the second would drop only the selection
  * and leave the argument list behind, as in `IO(a / b)(_ => IO.pure(0))`, which cannot compile.
  */
class ErrorHandlingMutator extends CustomMutator {
  private def isAppliedCombinator(name: String): Boolean =
    name == "handleErrorWith" || name == "handleError" || name == "recover" || name == "recoverWith"

  private def isNoArgCombinator(name: String): Boolean =
    name == "attempt" || name == "rethrow"

  def matcher: MutationMatcher = {
    case term @ Term.Apply.After_4_6_0(Term.Select(receiver, Term.Name(name)), _) if isAppliedCombinator(name) =>
      mutate(term, receiver, name)
    case term @ Term.Select(receiver, Term.Name(name)) if isNoArgCombinator(name) =>
      mutate(term, receiver, name)
  }

  private def mutate(term: Term, receiver: Term, combinatorName: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val metadata = MutantMetadata(s".$combinatorName", "", "ErrorHandling", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(term, receiver), metadata)))
  }
}
