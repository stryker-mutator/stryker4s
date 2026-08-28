package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: swaps sequential and parallel cats combinators.
  *
  * `orders.traverse(f)` becomes `orders.parTraverse(f)` and vice versa, likewise for `traverse_`, `flatTraverse`,
  * `sequence`, `sequence_` and `mapN`.
  *
  * Making sequential code parallel is the more interesting direction: it survives only if nothing depends on the
  * effects running in order, so a killed mutant is evidence that ordering is actually asserted somewhere. The reverse
  * direction catches code that is parallel by habit rather than by need.
  *
  * Note that `parTraverse` and friends require a `Parallel` instance, so the sequential-to-parallel direction can
  * produce compile errors for effect types that have none (`Either`, `Try`, plain `Option`). Those surface as
  * `CompileError` mutants, which Stryker4s reports but excludes from the mutation score.
  *
  * The applied combinators (`traverse`) and the no-argument ones (`sequence`) are kept in separate groups on purpose:
  * matching a bare `Term.Select` for an applied combinator would emit a second, duplicate mutant for the inner
  * selection of `xs.traverse(f)`.
  */
class ParallelSequentialSwapMutator extends CustomMutator {
  def matcher: MutationMatcher = {
    case term @ Term.Apply.After_4_6_0(select @ Term.Select(_, name @ Term.Name(method)), _)
        if isAppliedCombinator(method) =>
      val swapped = swap(method)
      val replacement =
        term.copyWithComments(fun = select.copyWithComments(name = name.copyWithComments(value = swapped)))
      mutate(term, replacement, method, swapped)

    case term @ Term.Select(_, name @ Term.Name(method)) if isNoArgCombinator(method) =>
      val swapped = swap(method)
      mutate(term, term.copyWithComments(name = name.copyWithComments(value = swapped)), method, swapped)
  }

  private def isAppliedCombinator(name: String): Boolean =
    name == "traverse" || name == "parTraverse" ||
      name == "traverse_" || name == "parTraverse_" ||
      name == "flatTraverse" || name == "parFlatTraverse" ||
      name == "mapN" || name == "parMapN"

  private def isNoArgCombinator(name: String): Boolean =
    name == "sequence" || name == "parSequence" ||
      name == "sequence_" || name == "parSequence_"

  /** `traverse` <-> `parTraverse`, driven purely by the `par` prefix. */
  private def swap(name: String): String =
    if (name.startsWith("par") && name.length > 3) decapitalize(name.substring(3))
    else "par" + capitalize(name)

  private def capitalize(name: String): String =
    name.substring(0, 1).toUpperCase + name.substring(1)

  private def decapitalize(name: String): String =
    name.substring(0, 1).toLowerCase + name.substring(1)

  private def mutate(term: Term, replacement: Term, from: String, to: String)(
      placeableTree: PlaceableTree
  ): Either[IgnoredMutations, Mutations] = {
    val metadata = MutantMetadata(from, to, "ParallelSequentialSwap", term.pos, None)
    Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(term, replacement), metadata)))
  }
}
