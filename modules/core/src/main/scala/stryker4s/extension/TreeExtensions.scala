package stryker4s.extension

import cats.Eq
import cats.syntax.all.*
import mutationtesting.Location
import mutationtesting.cats.*

import scala.annotation.tailrec
import scala.meta.*
import scala.reflect.ClassTag

object TreeExtensions {

  /** Searches up the tree for the closest parent of type `T`, if any. */
  @tailrec
  private def parentOfType[T <: Tree](tree: Tree)(implicit classTag: ClassTag[T]): Option[T] =
    tree.parent match {
      case Some(value: T)   => value.some
      case Some(otherValue) => parentOfType(otherValue)
      case _                => none
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
      thisTree.dfsCollectFirst {
        case found: T if found === toFind => found
      }

  }

  implicit final class TransformOnceExtension(val thisTree: Tree) extends AnyVal {

    /** The normal <code>Tree#transform</code> recursively transforms the tree each time a transformation is applied.
      * This causes a StackOverflowError when the transformation that is searched for is also present in the newly
      * transformed tree.
      *
      * This function does not recursively go into the transformed tree
      *
      * Note that scalameta's `Transformer` visits children before their parents, so `fn` is also called for nested
      * trees of a tree it transforms. Only the outermost transformation ends up in the resulting tree, but `fn` should
      * not assume it is only called for outermost trees (for example when it has side effects).
      */
    final def transformOnce(fn: PartialFunction[Tree, Tree]): Tree =
      new OnceTransformer(fn).transform(thisTree)

  }

  private class OnceTransformer(fn: PartialFunction[Tree, Tree]) extends Transformer {
    override protected def apply(tree: Tree): Tree = fn.applyOrElse(tree, super.apply)
  }

  implicit final class TreeIsInExtension(val thisTree: Tree) extends AnyVal {

    /** Returns if a tree is contained in a tree of type `[T]`. Recursively going up the tree until an annotation is
      * found.
      */
    final def isIn[T <: Tree](implicit classTag: ClassTag[T]): Boolean =
      parentOfType[T](thisTree).isDefined
  }

  implicit final class AncestorsExtension(val thisTree: Tree) extends AnyVal {

    /** The ancestors of this tree, from the immediate parent up to (and including) `root`. This tree itself is not
      * included.
      */
    final def ancestorsUpTo(root: Tree): Seq[Tree] = {
      val builder = Vector.newBuilder[Tree]
      val _ = existsAncestorUpTo(root) { ancestor => builder += ancestor; false }
      builder.result()
    }

    /** Whether any ancestor up to (and including) `root` matches `f`, without building the ancestor chain. */
    final def existsAncestorUpTo(root: Tree)(f: Tree => Boolean): Boolean = {
      @tailrec
      def loop(tree: Tree, root: Tree, f: Tree => Boolean): Boolean =
        tree.parent match {
          case Some(parent) => f(parent) || (parent ne root) && loop(parent, root, f)
          case None         => false
        }
      loop(thisTree, root, f)
    }
  }

  implicit final class GetMods(val tree: Tree) extends AnyVal {
    final def getMods: List[Mod] =
      tree match {
        case mc: Stat.WithMods           => mc.mods
        case mc: Term.MatchLike          => mc.mods
        case mc: Member.Param            => mc.mods
        case mc: Ctor.Primary            => mc.mods
        case mc: Type.FunctionParamOrArg => mc.mods
        case _                           => Nil
      }

  }

  /** Structural equality for `Tree`s
    */
  implicit def treeEq[A <: Tree]: Eq[A] = Eq.instance((x, y) => (x eq y) || structurallyEqual(x, y))

  /** Compares two trees by their structure (class, leaf values and children, recursively)
    */
  private def structurallyEqual(x: Tree, y: Tree): Boolean =
    (x eq y) ||
      (x.getClass == y.getClass &&
        ((x, y) match {
          case (x: Name, y: Name) => x.value == y.value
          case (x: Lit, y: Lit)   => x.value == y.value
          case _                  => true
        }) && childrenStructurallyEqual(x, y))

  /** `childrenCount` is allocation-free, so comparing it first avoids building the `children` lists of the (many) leaf
    * nodes and of nodes that can't match anyway
    */
  private def childrenStructurallyEqual(x: Tree, y: Tree): Boolean = {
    val childCount = x.childrenCount
    childCount == y.childrenCount &&
    (childCount == 0 || x.children.corresponds(y.children)(structurallyEqual(_, _)))
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
      val builder = Vector.newBuilder[T]

      def traverse(tree: Tree, inherited: => Option[C]): Unit = {
        // The context for this node and its descendants, only computed when a node actually collects something
        lazy val context: Option[C] = buildContextLifted(tree).orElse(inherited)
        collectFnLifted(tree).foreach(collect => context.foreach(c => builder += collect(c)))
        tree.foreachChild(traverse(_, context))
      }

      // Traverse the tree, starting with an empty context
      traverse(tree, None)
      builder.result()
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
