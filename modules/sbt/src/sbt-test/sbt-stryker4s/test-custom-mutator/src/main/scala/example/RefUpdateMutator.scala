package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: neutralises a `Ref` update by replacing its function with `identity`.
  *
  * `counter.update(_ + 1)` becomes `counter.update(identity)`, so the effect still runs, still has
  * the same type, and still touches the same `Ref` — but leaves the state unchanged. A surviving
  * mutant means nothing ever reads back the updated state, which is the usual shape of an untested
  * accumulator, cache or metric counter.
  *
  * Unlike the other examples this one rewrites an *argument* rather than the method name or the
  * enclosing call, which keeps the mutant type-correct without needing to know the effect type.
  * `identity` is used because it is always in scope via `Predef`, so the replacement never needs a
  * new import.
  *
  * `modify` is deliberately not handled: its function returns a `(state, result)` pair, so there is
  * no total identity-shaped replacement. An update that is already `identity` is skipped, since
  * that mutant would be equivalent to the original.
  */
class RefUpdateMutator extends CustomMutator {
  def matcher: MutationMatcher = {
    case term @ Term.Apply.After_4_6_0(Term.Select(_, Term.Name(method)), args)
        if isUpdate(method) && args.values.size == 1 && !isIdentity(args.values.head) =>
      mutate(term, term.copyWithComments(args = Term.ArgClause(Term.Name("identity") :: Nil)), method)
  }

  private def isUpdate(name: String): Boolean =
    name == "update" || name == "updateAndGet" || name == "getAndUpdate"

  private def isIdentity(arg: Term): Boolean =
    arg match {
      case Term.Name("identity") => true
      case _                     => false
    }

  private def mutate(term: Term, replacement: Term, method: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val metadata = MutantMetadata(method, "identity", "RefUpdate", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(term, replacement), metadata)))
  }
}
