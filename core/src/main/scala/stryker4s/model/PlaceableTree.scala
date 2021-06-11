package stryker4s.model

import scala.meta.Tree

/** A `Tree` where a mutation can be placed
  */
final case class PlaceableTree(tree: Tree) extends AnyVal
