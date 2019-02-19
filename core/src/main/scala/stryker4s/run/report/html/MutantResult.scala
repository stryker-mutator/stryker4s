package stryker4s.run.report.html

final case class MutantResult(id: String,
                              mutatorName: String,
                              replacement: String,
                              location: Location,
                              status: MutantStatus)
