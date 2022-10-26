package stryker4s.model

import scala.concurrent.duration.FiniteDuration

final case class RunResult(results: MutantResultsPerFile, duration: FiniteDuration)
