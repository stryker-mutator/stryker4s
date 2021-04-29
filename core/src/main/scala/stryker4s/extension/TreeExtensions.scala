package stryker4s.extension

import scala.annotation.tailrec
import scala.meta._
import scala.meta.transversers.SimpleTraverser
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

    /** Returns the statement this tree is part of. Recursively going up the tree until a full statement is found.
      */
    @tailrec
    final def topStatement(): Term =
      thisTerm match {
        case ParentIsPatternMatch(parent) => parent.topStatement()
        case ParentIsFullStatement()      => thisTerm
        case ParentIsTerm(parent)         => parent.topStatement()
        case _                            => thisTerm
      }

    /** Extractor object to check if the [[scala.meta.Term]] part of a pattern match (but not in the body of the pattern
      * match)
      */
    private object ParentIsPatternMatch {

      /** Go up the tree, until a Case is found (except for try-catches), then go up until a `Term` is found
        */
      final def unapply(term: Term): Option[Term] =
        findParent[Case](term)
          .filterNot(caze => caze.parent.collect { case t: Term.Try => t }.exists(_.catchp.contains(caze)))
          .flatMap(findParent[Term])

      private def findParent[T <: Tree](tree: Tree)(implicit classTag: ClassTag[T]): Option[T] =
        mapParent[T, Option[T]](tree, Some(_), None)
    }

    /** Extractor object to check if the direct parent of the [[scala.meta.Term]] is a 'full statement'
      */
    private object ParentIsFullStatement {
      final def unapply(term: Term): Boolean =
        term.parent exists {
          case _: Term.Assign                                   => true
          case _: Defn                                          => true
          case p if p.parent.exists(_.isInstanceOf[Term.Apply]) => false
          case _: Term.Block                                    => true
          case _: Term.If                                       => true
          case _: Term.ForYield                                 => true
          case _                                                => false
        }
    }

    private object ParentIsTerm {
      final def unapply(term: Term): Option[Term] =
        term.parent collect { case parent: Term =>
          parent
        }
    }
  }

  implicit class FindExtension(thisTree: Tree) {

    /** Searches for the given statement in the tree
      *
      * @param toFind
      *   Statement to find
      * @return
      *   A <code>Some(Tree)</code> if the statement has been found, otherwise None
      */
    def find[T <: Tree](toFind: T)(implicit classTag: ClassTag[T]): Option[T] =
      thisTree.collectFirst {
        case found: T if found.isEqual(toFind) => found
      }
  }

  implicit class TransformOnceExtension(thisTree: Tree) {

    /** The normal <code>Tree#transform</code> recursively transforms the tree each time a transformation is applied.
      * This causes a StackOverflowError when the transformation that is searched for is also present in the newly
      * transformed tree.
      *
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

    /** Returns if a tree is contained in a tree of type `[T]`. Recursively going up the tree until an annotation is
      * found.
      */
    final def isIn[T <: Tree](implicit classTag: ClassTag[T]): Boolean =
      mapParent[T, Boolean](thisTree, _ => true, false)
  }

  implicit class PathToRoot(thisTree: Tree) {
    class LeafToRootTraversable(t: Tree) extends Iterable[Tree] {
      @tailrec
      private def recTraverse[U](rt: Tree)(f: Tree => U): Unit = {
        f(rt)
        if (rt.parent.isDefined)
          recTraverse[U](rt.parent.get)(f)
      }
      override def foreach[U](f: Tree => U): Unit = recTraverse(t)(f)

      override def iterator: Iterator[Tree] =
        new Iterator[Tree] {
          var currentElement = Option(t)
          override def hasNext: Boolean = currentElement.flatMap(_.parent).isDefined

          override def next(): Tree = {
            currentElement = currentElement.flatMap(_.parent)
            currentElement.get
          }
        }
    }
    def pathToRoot: Iterable[Tree] = new LeafToRootTraversable(thisTree) {}
  }

  implicit class GetMods(tree: Tree) {
    def getMods: List[Mod] =
      tree match {
        case mc: Defn.Class  => mc.mods
        case mc: Defn.Trait  => mc.mods
        case mc: Defn.Object => mc.mods
        case mc: Defn.Def    => mc.mods
        case mc: Defn.Val    => mc.mods
        case mc: Defn.Var    => mc.mods
        case mc: Defn.Type   => mc.mods
        case mc: Term.Param  => mc.mods
        case mc: Decl.Def    => mc.mods
        case mc: Decl.Var    => mc.mods
        case mc: Decl.Val    => mc.mods
        case mc: Decl.Type   => mc.mods
        case _               => Nil
      }

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
