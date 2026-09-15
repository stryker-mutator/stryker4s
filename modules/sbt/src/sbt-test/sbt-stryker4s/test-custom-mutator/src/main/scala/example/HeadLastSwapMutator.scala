package example

import cats.data.NonEmptyVector
import stryker4s.mutatorapi.*

import scala.meta.*

/** Example custom mutator: swaps collection endpoints.
  *
  * `.head` becomes `.last`, `.headOption` becomes `.lastOption`, `.init` becomes `.tail`, and each swap also applies in
  * reverse.
  *
  * Off-by-one-at-the-end is a classic bug, and it is invisible to a test whose fixture happens to be a singleton or a
  * list of identical elements — which is what most fixtures are. Killing these mutants forces a fixture whose first and
  * last elements actually differ.
  *
  * Unlike [[CollectionOrderMutator]] and [[StringNormalizationMutator]], this mutator *renames* a selection rather than
  * dropping it, so it needs no guard against being the function of an enclosing `Term.Apply`: `xs.last(i)` compiles
  * just as `xs.head(i)` does.
  */
class HeadLastSwapMutator extends CustomMutator {
  private def swapped(name: String): String =
    if (name == "head") "last"
    else if (name == "last") "head"
    else if (name == "headOption") "lastOption"
    else if (name == "lastOption") "headOption"
    else if (name == "init") "tail"
    else if (name == "tail") "init"
    else ""

  def matcher: MutationMatcher = {
    case term @ Term.Select(receiver, Term.Name(name)) if swapped(name).nonEmpty =>
      placeableTree => {
        val replacementName = swapped(name)
        val replacement = Term.Select(receiver, Term.Name(replacementName))
        val metadata = MutantMetadata(name, replacementName, "HeadLastSwap", term.pos, None)
        Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(term, replacement), metadata)))
      }
  }
}
