package stryker4s.run.report.html

object MutationResultHealth extends Enumeration {

  type MutationResultHealth = Value
  val danger, warning, ok = Value
}
