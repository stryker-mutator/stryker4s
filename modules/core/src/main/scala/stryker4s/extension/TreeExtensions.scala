package stryker4s.extension

import cats.Eval
import cats.data.{Chain, State}
import cats.syntax.all.*
import mutationtesting.Location
import mutationtesting.cats.*

import scala.annotation.tailrec
import scala.meta.*
import scala.meta.transversers.SimpleTraverser
import scala.reflect.ClassTag

object TreeExtensions {
  @tailrec
  private def mapParent[T <: Tree, U](tree: Tree, ifFound: T => U, notFound: => U)(implicit classTag: ClassTag[T]): U =
    tree.parent match {
      case Some(value: T)   => ifFound(value)
      case Some(otherValue) => mapParent(otherValue, ifFound, notFound)
      case _                => notFound
    }

  implicit final class FindExtension(val thisTree: Tree) extends AnyVal {

    /** Searches for the given statement in the tree
      *
      * @param toFind
      *   Statement to find
      * @return
      *   A <code>Some(Tree)</code> if the statement has been found, otherwise None
      */
    final def find[T <: Tree](toFind: T)(implicit classTag: ClassTag[T]): Option[T] =
      thisTree.collectFirst {
        case found: T if found.isEqual(toFind) => found
      }

  }

  implicit final class TransformOnceExtension(val thisTree: Tree) extends AnyVal {

    /** The normal <code>Tree#transform</code> recursively transforms the tree each time a transformation is applied.
      * This causes a StackOverflowError when the transformation that is searched for is also present in the newly
      * transformed tree.
      *
      * This function does not recursively go into the transformed tree
      */
    final def transformOnce(fn: PartialFunction[Tree, Tree]): Tree = {
      val onceTransformer = new OnceTransformer(fn)
      onceTransformer(thisTree)
    }

    /** Tries to transform a tree exactly once, returning None if the transformation was never applied
      */
    final def transformExactlyOnce(fn: PartialFunction[Tree, Tree]): Option[Tree] = {
      var isTransformed = false
      val checkFn = fn.andThen { t =>
        isTransformed = true
        t
      }
      val onceTransformer = new OnceTransformer(checkFn)
      val result = onceTransformer(thisTree)

      isTransformed.guard[Option].as(result)
    }
  }

  private class OnceTransformer(fn: PartialFunction[Tree, Tree]) extends Transformer {
    override def apply(tree: Tree): Tree = {
      fn.applyOrElse(tree, super.apply)
    }
  }

  implicit final class TreeIsInExtension(val thisTree: Tree) extends AnyVal {

    /** Returns if a tree is contained in a tree of type `[T]`. Recursively going up the tree until an annotation is
      * found.
      */
    final def isIn[T <: Tree](implicit classTag: ClassTag[T]): Boolean =
      mapParent[T, Boolean](thisTree, _ => true, false)
  }

  implicit final class GetMods(val tree: Tree) extends AnyVal {
    final def getMods: List[Mod] =
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

  implicit final class IsEqualExtension(val thisTree: Tree) extends AnyVal {

    /** Structural equality for Trees
      */
    final def isEqual(other: Tree): Boolean = thisTree == other || thisTree.structure == other.structure
  }

  implicit final class CollectFirstExtension(tree: Tree) {
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

  implicit final class CollectWithContextExtension(val tree: Tree) extends AnyVal {

    /** Scalameta collector that collects on a PartialFunction, but can build up a 'context' object that is passed to
      * each node
      */
    final def collectWithContext[T, C](
        buildContext: PartialFunction[Tree, C]
    )(collectFn: PartialFunction[Tree, C => T]): Seq[T] = {
      val collectFnLifted = collectFn.lift
      val buildContextLifted = buildContext.lift

      def traverse(tree: Tree): State[Eval[Option[C]], Chain[T]] =
        State.get[Eval[Option[C]]].flatMap { inherited =>
          // The context for this node and its descendants
          val contextEval = Eval.defer(buildContextLifted(tree).fold(inherited)(_.some.pure[Eval])).memoize

          // Only read the context when this node actually collects something.
          val collected = collectFnLifted(tree).foldMap(collect => Chain.fromOption(contextEval.value.map(collect)))

          // Each child starts from this node's context, independent of its siblings.
          tree.children.foldMapM(child => State.set(contextEval) *> traverse(child)).map(collected ++ _)
        }

      // Traverse the tree, starting with an empty context
      traverse(tree).runA(none.pure[Eval]).value.toVector
    }

  }

  implicit final class PositionExtension(val pos: Position) extends AnyVal {

    /** Map a `scala.meta.Position` to a `mutationtesting.Location`
      */
    def toLocation: Location = Location(
      start = mutationtesting.Position(line = pos.startLine + 1, column = pos.startColumn + 1),
      end = mutationtesting.Position(line = pos.endLine + 1, column = pos.endColumn + 1)
    )
  }

  implicit final class LocationExtension(val pos: Location) extends AnyVal {
    import mutationtesting.Position

    /** Adds an offset to a `mutationtesting.Location`
      */
    def withOffset(offset: Location, stringValue: Lit.String): Location = {
      val stringOffset = if (stringValue.text.startsWith("\"\"\"")) 3 else 1
      Location(
        offset.start |+| Position(0, stringOffset),
        offset.start |+| Position(0, stringOffset)
      ) |+| pos
    }
  }
}
