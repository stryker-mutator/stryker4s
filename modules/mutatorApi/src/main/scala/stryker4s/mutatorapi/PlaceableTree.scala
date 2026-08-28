package stryker4s.mutatorapi

import scala.meta.{Term, Transformer, Tree}
import scala.meta.prettyprinters.{XtensionStructure, XtensionSyntax}

/** A `Tree` where a mutation can be placed
  */
final case class PlaceableTree(tree: Tree) extends AnyVal {
  override def toString() = tree.toString()

  /** Builds the full mutated statement for a mutation, by replacing `original` with `replacement` inside this tree.
    *
    * A [[CustomMutator]] usually matches on a small sub-term (say the `IO.sleep(d)` inside
    * `IO.sleep(d).as(q).timeout(l)`), but a [[MutatedCode]] must carry the *whole* placeable statement, with the
    * mutation applied in place. Returning only the replacement sub-term produces a mutant that does not compile
    * whenever the matched term is not the entire placeable tree.
    *
    * @throws IllegalArgumentException
    *   if `original` cannot be found inside this tree, or if substituting into it does not yield a `Term`.
    */
  def substitute(original: Term, replacement: Term): Term = {
    val target = PlaceableTree
      .find(tree, original)
      .getOrElse(
        throw new IllegalArgumentException(
          s"Could not find '${original.syntax}' in '${tree.syntax}'"
        )
      )

    val transformer = new Transformer {
      override protected def apply(t: Tree): Tree = if (t eq target) replacement else super.apply(t)
    }

    transformer.transform(tree) match {
      case t: Term => t
      case t       =>
        throw new IllegalArgumentException(
          s"Replacing '${original.syntax}' with '${replacement.syntax}' in '${tree.syntax}' did not produce a Term, but a ${t.getClass.getSimpleName}"
        )
    }
  }
}

object PlaceableTree {

  /** Depth-first search for `original` within `tree`, by reference identity first and position plus structure second (a
    * matcher may hand back a re-created, but equivalent, term).
    */
  private def find(tree: Tree, original: Term): Option[Tree] =
    if ((tree eq original) || (tree.pos == original.pos && tree.structure == original.structure)) Some(tree)
    else tree.children.iterator.map(find(_, original)).collectFirst { case Some(found) => found }
}
