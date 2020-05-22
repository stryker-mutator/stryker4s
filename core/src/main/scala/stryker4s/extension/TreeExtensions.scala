package stryker4s.extension

import scala.annotation.tailrec
import scala.meta.transversers.SimpleTraverser
import scala.meta.{Case, Lit, Term, Transformer, Tree}
import scala.reflect.ClassTag
import scala.util.Try

object TreeExtensions {
  @tailrec
  private def mapParent[T <: Tree, U](tree: Tree, ifFound: T => U, notFound: => U)(implicit classTag: ClassTag[T]): U =
    tree.parent match {
      case Some(value: T)   => ifFound(value)
      case Some(otherValue) => mapParent(otherValue, ifFound, notFound)
      case _                => notFound
    }

  implicit class TopStatementExtension(thisTerm: Term) {

    /** Returns the statement this tree is part of.
      * Recursively going up the tree until a full statement is found.
      */
    @tailrec
    final def topStatement(): Term =
      thisTerm match {
        case ParentIsPatternMatch(parent)  => parent
        case ParentIsNotExpression(parent) => parent
        case _: Lit                        => thisTerm
        case PartialStatement(parent)      => parent.topStatement()
        case _                             => thisTerm
      }

    /** Extractor object to check if a [[scala.meta.Term]] is part of a statement or a full one.
      *
      */
    private object PartialStatement {

      /**
        * @return A Some of the parent if the given term is a partial statement,
        *         else a None if the given term is a full statement
        */
      final def unapply(term: Term): Option[Term] =
        term.parent collect {
          case parent: Term.Name       => parent
          case parent: Term.Apply      => parent
          case parent: Term.Select     => parent
          case parent: Term.ApplyType  => parent
          case parent: Term.ApplyInfix => parent
          case parent: Term.Match      => parent
        }
    }

    /** Extractor object to check if the [[scala.meta.Term]] is inside a pattern match
      *
      */
    private object ParentIsPatternMatch {

      /** Go up the tree, until a Case is found, then go up until a `Term` is found
        *
        */
      final def unapply(term: Term): Option[Term] = findParent[Case](term) flatMap findParent[Term]

      private def findParent[T <: Tree](tree: Tree)(implicit classTag: ClassTag[T]): Option[T] =
        mapParent[T, Option[T]](tree, Some(_), None)
    }

    /** If the parent is a `!...` expression
      */
    private object ParentIsNotExpression {
      final def unapply(term: Term): Option[Term] =
        term.parent collect {
          case parent @ Term.ApplyUnary(Term.Name("!"), _) => parent
        }
    }
  }

  implicit class FindExtension(thisTree: Tree) {

    /** Searches for the given statement in the tree
      *
      * @param toFind Statement to find
      * @return A <code>Some(Tree)</code> if the statement has been found, otherwise None
      */
    def find[T <: Tree](toFind: T)(implicit classTag: ClassTag[T]): Option[T] =
      thisTree.collectFirst {
        case found: T if found.isEqual(toFind) => found
      }
  }

  implicit class TransformOnceExtension(thisTree: Tree) {

    /** The normal <code>Tree#transform</code> recursively transforms the tree each time a transformation is applied
      * This causes a StackOverflowError when the transformation that is searched for is also present in the newly transformed tree. <br>
      * This function does not recursively go into the transformed tree
      */
    def transformOnce(fn: PartialFunction[Tree, Tree]): Try[Tree] = {
      Try {
        val onceTransformer = new OnceTransformer(fn)
        onceTransformer(thisTree)
      }
    }

    private class OnceTransformer(fn: PartialFunction[Tree, Tree]) extends Transformer {
      override def apply(tree: Tree): Tree = fn.applyOrElse(tree, super.apply)
    }
  }

  implicit class TreeIsInExtension(thisTree: Tree) {

    /** Returns if a tree is contained in an tree of type `[T]`.
      * Recursively going up the tree until an annotation is found.
      */
    final def isIn[T <: Tree](implicit classTag: ClassTag[T]): Boolean =
      mapParent[T, Boolean](thisTree, _ => true, false)
  }

  implicit class IsEqualExtension(thisTree: Tree) {

    /** Structural equality for Trees
      */
    final def isEqual(other: Tree): Boolean = thisTree == other || thisTree.structure == other.structure
  }

  implicit class CollectFirstExtension(tree: Tree) {
    final def collectFirst[T](pf: PartialFunction[Tree, T]): Option[T] = {
      var result = Option.empty[T]
      object traverser extends SimpleTraverser {
        override def apply(t: Tree): Unit = {
          if (result.isEmpty && pf.isDefinedAt(t)) {
            result = Some(pf(t))
          } else if (result.isEmpty) {
            super.apply(t)
          }
        }
      }
      traverser(tree)
      result
    }
  }
}
