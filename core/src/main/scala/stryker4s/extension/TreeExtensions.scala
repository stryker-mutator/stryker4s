package stryker4s.extension

import cats.syntax.option.*
import mutationtesting.Location

import scala.annotation.tailrec
import scala.meta.*
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

  implicit class TopStatementExtension(val thisTerm: Term) extends AnyVal {

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
  }

  /** Extractor object to check if the [[scala.meta.Term]] part of a pattern match (but not in the body of the pattern
    * match)
    */
  private object ParentIsPatternMatch {

    /** Go up the tree, until a Case is found (except for try-catches), then go up until a `Term` is found
      */
    final def unapply(term: Term): Option[Term] =
      term
        .findParent[Case]
        .filterNot(caze => caze.parent.collect { case t: Term.Try => t }.exists(_.catchp.contains(caze)))
        .flatMap(_.findParent[Term])

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

  implicit class FindExtension(val thisTree: Tree) extends AnyVal {

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

    def findParent[T <: Tree](implicit classTag: ClassTag[T]): Option[T] =
      mapParent[T, Option[T]](thisTree, Some(_), None)
  }

  implicit class TransformOnceExtension(val thisTree: Tree) extends AnyVal {

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

    /** Tries to transform a tree exactly once, returning None if the transformation was never applied
      */
    def transformExactlyOnce(fn: PartialFunction[Tree, Tree]): Option[Tree] = {
      var isTransformed = false
      val checkFn = fn.andThen { t =>
        isTransformed = true
        t
      }
      val onceTransformer = new OnceTransformer(checkFn)
      val result = onceTransformer(thisTree)

      if (isTransformed) result.some
      else None
    }
  }

  private class OnceTransformer(fn: PartialFunction[Tree, Tree]) extends Transformer {
    override def apply(tree: Tree): Tree = {
      val supered = super.apply(tree)
      fn.applyOrElse(supered, identity[Tree])
    }
  }

  implicit class TreeIsInExtension(val thisTree: Tree) extends AnyVal {

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

  implicit class GetMods(val tree: Tree) extends AnyVal {
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

  implicit class IsEqualExtension(val thisTree: Tree) extends AnyVal {

    /** Structural equality for Trees
      */
    final def isEqual(other: Tree): Boolean = thisTree == other || thisTree.structure == other.structure
  }

  implicit class CollectFirstExtension(tree: Tree) {
    final def collectFirst[T](pf: PartialFunction[Tree, T]): Option[T] = {
      var result = Option.empty[T]
      val fn = pf.lift
      object traverser extends SimpleTraverser {
        override def apply(t: Tree): Unit = {
          result = if (result.isEmpty) fn(t).orElse(result) else result
          super.apply(t)
        }
      }
      traverser(tree)
      result
    }
  }

  implicit class PositionExtension(val pos: Position) extends AnyVal {

    /** Map a `scala.meta.Position` to a `mutationtesting.Location`
      */
    def toLocation: Location = Location(
      start = mutationtesting.Position(line = pos.startLine + 1, column = pos.startColumn + 1),
      end = mutationtesting.Position(line = pos.endLine + 1, column = pos.endColumn + 1)
    )
  }
}
