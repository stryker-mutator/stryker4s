package stryker4s.run.report.html
import stryker4s.run.report.html.MutationResultHealth.MutationResultHealth

sealed trait MutationTestResult {
  val name: String
  val totals: ResultTotals
  val mutationScore: Double
  val health: MutationResultHealth
  val path: String
}

final case class FileResult(source: String,
                            mutants: Seq[MutantResult],
                            name: String,
                            totals: ResultTotals,
                            mutationScore: Double,
                            health: MutationResultHealth,
                            path: String)
    extends MutationTestResult {
  val language: String = "scala"
}

final case class DirectoryResult(childResults: Seq[MutationTestResult],
                                 name: String,
                                 totals: ResultTotals,
                                 mutationScore: Double,
                                 health: MutationResultHealth,
                                 path: String)
    extends MutationTestResult
