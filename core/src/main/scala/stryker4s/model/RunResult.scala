package stryker4s.model

import stryker4s.model.MutantResultsPerFile

import scala.concurrent.duration.FiniteDuration

final case class RunResult(results: MutantResultsPerFile, duration: FiniteDuration)
