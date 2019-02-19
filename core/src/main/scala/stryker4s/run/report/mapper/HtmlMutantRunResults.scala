package stryker4s.run.report.mapper

final case class Position(line: Int, column: Int)
final case class Location(start: Position, end: Position )
final case class Totals(detected: Int, undetected: Int, valid: Int, invalid: Int)
final case class HtmlMutant(id: String, mutatorName: String, replacement: String, location: Location, status: String)
final case class HtmlMutantRunResult(name: String, path: String, totals: Totals, health: String, language: String, source: String, mutants: List[HtmlMutant])
final case class HtmlMutantRunResults(name: String, path: String, totals: Totals, health: String, childResults: List[HtmlMutantRunResult])
