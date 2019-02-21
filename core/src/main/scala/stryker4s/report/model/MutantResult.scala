package stryker4s.report.model
import stryker4s.report.model.MutantStatus.MutantStatus

final case class MutantResult(id: String,
                              mutatorName: String,
                              replacement: String,
                              location: Location,
                              status: MutantStatus)
