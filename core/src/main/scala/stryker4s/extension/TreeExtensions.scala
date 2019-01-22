package stryker4s.extension

import scala.annotation.tailrec
import scala.meta.contrib._
import scala.meta.{Case, Term, Transformer, Tree}
import scala.reflect.ClassTag
import scala.util.Try

object TreeExtensions {

  implicit class TopStatementExtension(thisTerm: Term) {

    /** Returns the statement this tree is part of.
      * Recursively going up the tree until a full statement is found.
      */
    @tailrec
    final def topStatement(): Term = thisTerm match {
      case PartialStatement(parent)     => parent.topStatement()
      case ParentIsPatternMatch(parent) => parent
      case _                            => thisTerm
    }

    /** Extractor object to check if a [[scala.meta.Term]] is part of a statement or a full one.
      *
      */
    private object PartialStatement {

      /**
        * @return A Some of the parent if the given term is a partial statement,
        *         else a None if the given term is a full statement
        */
      final def unapply(term: Term): Option[Term] = term.parent collect {
        case parent: Term.Apply      => parent
        case parent: Term.Select     => parent
        case parent: Term.ApplyType  => parent
        case parent: Term.ApplyInfix => parent
      }
    }

    /** Extractor object to check if the [[scala.meta.Term]] is inside a pattern match
      *
      */
    private object ParentIsPatternMatch {

      final def unapply(term: Term): Option[Term] = if (term.isIn[Case]) parentOf(term) else None

      /** Go up the tree, until a Case is found, then go up until a `Term` is found
        *
        */
      @tailrec
      private def parentOf(tree: Tree): Option[Term] = tree.parent match {
        case Some(caseTree: Case) => // Case is found, go into that
          caseTree.parent match {
            case Some(term: Term) => Some(term) // Great success!
            case Some(other)      => parentOf(other) // Keep going up
            case None             => None
          }
        case Some(other) => parentOf(other) // Keep going up
        case None        => None // Top of tree is reached, we better stop
      }

    }
  }

  implicit class FindExtension(thisTree: Tree) {

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
  }

  implicit class TransformOnceExtension(thisTree: Tree) {

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

  implicit class TreeIsInExtension(thisTree: Tree) {

    /** Returns if a tree is contained in an tree of type `[T]`.
      * Recursively going up the tree until an annotation is found.
      */
    @tailrec
    final def isIn[T <: Tree](implicit classTag: ClassTag[T]): Boolean = thisTree.parent match {
      case Some(_: T)        => true
      case Some(value: Tree) => value.isIn[T]
      case _                 => false
    }
  }
}
