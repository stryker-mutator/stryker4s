package stryker4s.plugin.api

import stryker4s.plugin.api.internal.TreeEquality

import scala.meta.prettyprinters.XtensionSyntax
import scala.meta.{Term, Transformer, Tree}

/** A `Tree` where a mutation can be placed
  */
final case class PlaceableTree(tree: Tree) extends AnyVal {
  override def toString() = tree.toString()

  /** Builds the full mutated statement for a mutation, by replacing `original` with `replacement` inside this tree.
    *
    * Matchers usually match a sub-term, but a [[MutatedCode]] must carry the whole statement with the mutation applied.
    *
    * Throws `IllegalArgumentException` if `original` is not inside this tree, or the substitution does not yield a
    * `Term`.
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
      override protected def replaceSubtree(t: Tree): Tree = if (t eq target) replacement else null
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
    if ((tree eq original) || (tree.pos == original.pos && TreeEquality.structurallyEqual(tree, original))) Some(tree)
    else tree.children.iterator.map(find(_, original)).collectFirst { case Some(found) => found }
}
