package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: removes cats-effect resource finalizers.
  *
  *   - `Resource.make(acquire)(release)` becomes `Resource.eval(acquire)`
  *   - `fa.bracket(use)(release)` becomes `fa.flatMap(use)`
  *   - `fa.guarantee(cleanup)` (and `guaranteeCase`, `onCancel`) becomes `fa`
  *
  * In every case the acquisition and the use of the resource are preserved and only the cleanup is dropped, so the
  * mutant is behaviourally identical unless a test actually observes the release — by asserting a connection was
  * returned to the pool, a file handle closed, a lock freed, and so on. Finalizers are some of the least-tested code in
  * an effectful codebase, so surviving mutants here are usually genuine coverage gaps rather than noise.
  *
  * `Resource.make` is only matched when its qualifier is literally `Resource` (bare or as the last segment of a
  * qualified path), so unrelated `Foo.make(a)(b)` builders are left alone.
  */
class ResourceFinalizerMutator extends CustomMutator {
  def matcher: MutationMatcher = {
    // Resource.make(acquire)(release) / Resource.makeCase(acquire)(release)
    case term @ Term.Apply.After_4_6_0(
          Term.Apply.After_4_6_0(Term.Select(qualifier, Term.Name(method)), acquire),
          _
        ) if isResourceQualifier(qualifier) && isResourceMake(method) && acquire.values.size == 1 =>
      val replacement = Term.Apply(Term.Select(qualifier, Term.Name("eval")), acquire)
      mutate(term, replacement, method)

    // fa.bracket(use)(release) / fa.bracketCase(use)(release)
    case term @ Term.Apply.After_4_6_0(
          Term.Apply.After_4_6_0(Term.Select(receiver, Term.Name(method)), use),
          _
        ) if isBracket(method) && use.values.size == 1 =>
      val replacement = Term.Apply(Term.Select(receiver, Term.Name("flatMap")), use)
      mutate(term, replacement, method)

    // fa.guarantee(cleanup) / fa.guaranteeCase(cleanup) / fa.onCancel(cleanup)
    case term @ Term.Apply.After_4_6_0(Term.Select(receiver, Term.Name(method)), args)
        if isGuarantee(method) && args.values.size == 1 =>
      mutate(term, receiver, method)
  }

  private def isResourceQualifier(qualifier: Term): Boolean =
    qualifier match {
      case Term.Name("Resource")                 => true
      case Term.Select(_, Term.Name("Resource")) => true
      case _                                     => false
    }

  private def isResourceMake(name: String): Boolean =
    name == "make" || name == "makeCase"

  private def isBracket(name: String): Boolean =
    name == "bracket" || name == "bracketCase"

  private def isGuarantee(name: String): Boolean =
    name == "guarantee" || name == "guaranteeCase" || name == "onCancel"

  private def mutate(term: Term, replacement: Term, removed: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val metadata = MutantMetadata(removed, "no finalizer", "ResourceFinalizer", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(term, replacement), metadata)))
  }
}
