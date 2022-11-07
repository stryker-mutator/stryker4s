package stryker4s.mutants

import cats.syntax.flatMap.*
import cats.syntax.option.*
import stryker4s.extensions.TreeExtensions.{FindExtension, TreeIsInExtension}
import stryker4s.extensions.mutationtype.ParentIsTypeLiteral
import stryker4jvm.logging.Logger

import scala.meta.*

trait Traverser {

  /** If the currently visiting node is a node where mutations can be placed, that node is returned, otherwise None
    */
  def canPlace(currentTree: Tree): Option[Term]

}

final class TraverserImpl(implicit log: Logger) extends Traverser {

  def canPlace(currentTree: Tree): Option[Term] = {
    val toPlace = currentTree match {
      case _: Term.Name                                             => none
      case t: Term.Match                                            => t.some
      case t: Case if t.cond.flatMap(_.find(currentTree)).isDefined => none
      case t: Term.Apply                                            => t.some
      case t: Term.ApplyInfix                                       => t.some
      case t: Term.Block                                            => t.some
      case t: Term.If                                               => t.some
      case t: Term.ForYield                                         => t.some
      case t: Term.Interpolate                                      => t.some
      case t: Lit                                                   => t.some
      case _                                                        => none
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
      .flatTap(t => log.debug(s"Found tree to place mutations: ${fansi.Color.Green(t.syntax)}").some)

  }
}
