package stryker4s.extension.mutationtype
import scala.meta.Term

case object PatternMatch {

  def unapply(tree: Term.Match): Option[(Term, Seq[PatternMatchMutation])] = {
    // We don't mutate a match that only has one case
    if (tree.cases.length > 1) {
      Some(
        tree,
        tree.cases.map { caze =>
          import scala.meta.quasiquotes.XtensionQuasiquoteTerm

          // https://www.scala-lang.org/api/2.12.1/scala/unchecked.html
          // @unchecked is required because we are removing cases here,
          // possibly resulting in compile warnings (which could be flagged as errors)
          val annotationWrap = q"${tree.expr}: @unchecked"
          PatternMatchMutation(tree.copy(expr = annotationWrap, cases = tree.cases.filterNot(_ == caze)))
        }
      )
    } else None
  }
  case class PatternMatchMutation(tree: Term.Match) extends PatternMatchExpression
}
