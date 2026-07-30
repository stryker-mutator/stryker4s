package stryker4s.mutants

import cats.syntax.all.*
import stryker4s.extension.TreeExtensions.TreeIsInExtension
import stryker4s.mutation.ParentIsTypeLiteral

import scala.meta.*

trait TreeTraverser {

  /** If the currently visiting node is a node where mutations can be placed, that node is returned, otherwise None
    */
  def canPlace(currentTree: Tree): Option[Term]

}

final class TreeTraverserImpl() extends TreeTraverser {

  def canPlace(currentTree: Tree): Option[Term] = currentTree match {
    case term: Term if canPlaceIn(term.parentOrNoTree, term) => term.some
    case _                                                   => none
  }

  private def canPlaceIn(parent: Tree, currentTree: Term): Boolean =
    (parent match {
      case Tree.NoTree                                       => false
      case ParentIsTypeLiteral()                             => false
      case name: Name                                        => !name.isDefinition
      case t if t.is[Init]                                   => false
      case d: Defn.Def if d.body == currentTree              => true
      case d: Defn.Val if d.rhs == currentTree               => true
      case d: Defn.Var if d.body == currentTree              => true
      case _: Term.Block                                     => true
      case t: Term.Function if t.body == currentTree         => true
      case t: Case if t.body == currentTree                  => true
      case t: Term.ForYield if t.body == currentTree         => true
      case t: Template.Body if t.stats.contains(currentTree) => true
      case _                                                 => false
    }) && !parent.isIn[Mod.Annot]
}
