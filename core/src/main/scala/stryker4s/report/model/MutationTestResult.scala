package stryker4s.report.model

final case class MutationTestResult(source: String, mutants: Seq[MutantResult], language: String = "scala")
