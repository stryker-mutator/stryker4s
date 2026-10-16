package stryker4s.plugin.api.internal

import cats.Eq

import scala.meta.{Lit, Name, Tree}

private[stryker4s] object TreeEquality {

  /** Structural equality for `Tree`s, ignoring positions */
  def treeEq[A <: Tree]: Eq[A] = Eq.instance((x, y) => structurallyEqual(x, y))

  def structurallyEqual(x: Tree, y: Tree): Boolean =
    (x eq y) ||
      (x.getClass == y.getClass &&
        ((x, y) match {
          case (x: Name, y: Name) => x.value == y.value
          case (x: Lit, y: Lit)   => x.value == y.value
          case _                  => true
        }) && childrenStructurallyEqual(x, y))

  // `childrenCount` is allocation-free, so leaves and mismatching nodes never build their `children` lists
  private def childrenStructurallyEqual(x: Tree, y: Tree): Boolean = {
    val childCount = x.childrenCount
    childCount == y.childrenCount &&
    (childCount == 0 || x.children.corresponds(y.children)(structurallyEqual(_, _)))
  }
}
