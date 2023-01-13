package stryker4jvm.mutator.scala

import scala.meta.Tree

/** A `Tree` where a mutation can be placed
  */
final case class PlaceableTree(tree: Tree) extends AnyVal {
  override def toString() = tree.toString()
}
