package stryker4s.mutants

import stryker4s.extension.TreeExtensions.{FindExtension, TreeIsInExtension}
import stryker4s.extension.mutationtype.ParentIsTypeLiteral

import scala.meta.*

trait Traverser {

  /** If the currently visiting node is a node where mutations can be placed, that node is returned, otherwise None
    */
  def canPlace(currentTree: Tree): Option[Term]

}

final class TraverserImpl extends Traverser {

  def canPlace(currentTree: Tree): Option[Term] = {
    val toPlace = currentTree match {
      case d: Defn.Def                                              => Some(d.body)
      case d: Defn.Val                                              => Some(d.rhs)
      case _: Term.Name                                             => None
      case t: Term.Match                                            => Some(t)
      case t: Case if t.cond.flatMap(_.find(currentTree)).isDefined => None
      case t: Term.Apply                                            => Some(t)
      case t: Term.ApplyInfix                                       => Some(t)
      case t: Term.Block                                            => Some(t)
      case t: Term.If                                               => Some(t)
      case t: Term.ForYield                                         => Some(t)
      case t: Lit                                                   => Some(t)
      case _                                                        => None
    }

    toPlace
      // Filter out all the node places that are invalid
      .filter {
        case name: Name => !name.isDefinition
        // Don't place inside `case` patterns or conditions
        case p
            if p.findParent[Case].exists(c => c.pat.contains(currentTree) || c.cond.exists(_.contains(currentTree))) =>
          false
        case t if t.parent.exists(_.is[Init])                              => false
        case t if t.parent.exists(p => p.is[Term] && p.isNot[Term.Select]) => false
        case ParentIsTypeLiteral()                                         => false
        case _                                                             => true
      }
      .filterNot(_.isIn[Mod.Annot])
  }
}
