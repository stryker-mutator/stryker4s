package stryker4s.extensions

import scala.annotation.tailrec
import scala.meta.contrib._
import scala.meta.internal.trees.InternalTree
import scala.meta.{Case, Defn, Lit, Mod, Source, Term, Transformer, Tree}
import scala.util.Try

object TreeExtensions {

  implicit class ImplicitTermExtensions(thisTerm: Term) {

    /** Returns the statement this tree is part of.
      * Recursively going up the tree until a full statement is found.
      */
    @tailrec
    final def topStatement(): Term = thisTerm match {
      case PartialStatement(parent)    => parent.topStatement()
      case LiteralPatternMatch(parent) => parent
      case _                           => thisTerm
    }

    /** Returns if a tree is contained in an annotation
      * Recursively going up the tree until a annotation is found.
      */
    final def isInAnnotation: Boolean = {
      @tailrec
      def loop(tree: Tree): Boolean = {
        tree.parent match {
          case Some(value) =>
            value match {
              case _: Mod.Annot   => true
              case _: Term.Param  => false
              case _: Defn.Def    => false
              case _: Defn.Class  => false
              case _: Defn.Object => false
              case _              => loop(value)
            }
          case _ => false
        }
      }
      loop(thisTerm)
    }

    /** Extractor object to check if a [[scala.meta.Term]] is part of a statement or a full one.
      *
      */
    private object PartialStatement {

      /**
        * @return A Some of the parent if the given term is a partial statement,
        *         else a None if the given term is a full statement
        */
      def unapply(term: Term): Option[Term] = term.parent collect {
        case parent: Term.Apply      => parent
        case parent: Term.Select     => parent
        case parent: Term.ApplyType  => parent
        case parent: Term.ApplyInfix => parent
      }
    }

    /** Extractor object to check if the [[scala.meta.Term]] is a literal inside a pattern match
      *
      */
    private object LiteralPatternMatch {
      def unapply(literal: Lit): Option[Term] = literal.parent match {
        case Some(parent: Case) =>
          parent.parent collect {
            case topParent: Term => topParent.topStatement()
          }
        case _ => None
      }
    }

  }

  implicit class ImplicitTreeExtensions(thisTree: Tree) {

    /** Searches for the given statement in the tree
      *
      * @param toFind Statement to find
      * @return A <code>Some(Tree)</code> if the statement has been found, otherwise None
      */
    def find[T <: Tree](toFind: T): Option[T] = thisTree.collectFirst {
      // We can safely cast because the structure is the same anyway.
      // The cast is done so the return type of this function is the same as the `toFind` parameter
      case found: Tree if found.isEqual(toFind) => found.asInstanceOf[T]
    }

    /** The normal <code>Tree#transform</code> recursively transforms the tree each time a transformation is applied
      * This causes a StackOverflowError when the transformation that is searched for is also present in the newly transformed tree. <br>
      * This function does not recursively go into the transformed tree
      */
    def transformOnce(fn: PartialFunction[Tree, Tree]): Try[Tree] = {
      Try {
        val liftedFn = fn.lift
        val transformer = new OnceTransformer(liftedFn)
        transformer(thisTree)
      }
    }

    private class OnceTransformer(liftedFn: Tree => Option[Tree]) extends Transformer {
      override def apply(tree: Tree): Tree =
        liftedFn(tree).getOrElse(super.apply(tree))
    }
  }
}
