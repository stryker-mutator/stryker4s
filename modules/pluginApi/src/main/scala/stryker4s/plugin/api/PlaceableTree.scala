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
    */
  def substitute(original: Term, replacement: Term): Either[SubstitutionFailed, Term] = {
    def failed(message: String) = SubstitutionFailed(original.syntax, tree.syntax, message)

    PlaceableTree.find(tree, original).toRight(failed("original not found in statement")).flatMap { target =>
      val transformer = new Transformer {
        override protected def replaceSubtree(t: Tree): Tree = if (t eq target) replacement else null
      }
      transformer.transform(tree) match {
        case t: Term => Right(t)
        case t       => Left(failed(s"result is a ${t.getClass.getSimpleName}, not a Term"))
      }
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
