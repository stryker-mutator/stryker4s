package stryker4s.run.report.mapper

case class Totals(detected: Int, undetected: Int, valid: Int, invalid: Int)
case class HtmlMutant(id: String, mutatorName: String, replacement: String, span: Array[Int], status: String)
case class HtmlMutantRunResult(name: String, path: String, totals: Totals, health: String, language: String, source: String, mutants: List[HtmlMutant])
case class HtmlMutantRunResults(name: String, path: String, totals: Object, health: String, childResults: List[HtmlMutantRunResult])
