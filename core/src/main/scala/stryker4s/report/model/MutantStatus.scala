package stryker4s.report.model

object MutantStatus extends Enumeration {
  type MutantStatus = Value
  val Killed, Survived, NoCoverage, CompileError, Timeout = Value
}
