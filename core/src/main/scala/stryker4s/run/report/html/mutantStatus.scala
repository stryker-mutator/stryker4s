package stryker4s.run.report.html

sealed trait MutantStatus

case object Killed extends MutantStatus
case object Survived extends MutantStatus
case object NoCoverage extends MutantStatus
case object CompileError extends MutantStatus
case object RuntimeError extends MutantStatus
case object Timeout extends MutantStatus
