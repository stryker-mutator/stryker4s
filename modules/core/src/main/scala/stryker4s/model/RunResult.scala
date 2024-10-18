package stryker4s.model

import mutationtesting.TestFileDefinitionDictionary

import scala.concurrent.duration.FiniteDuration

final case class RunResult(
    results: MutantResultsPerFile,
    testFiles: Option[TestFileDefinitionDictionary],
    duration: FiniteDuration
)
