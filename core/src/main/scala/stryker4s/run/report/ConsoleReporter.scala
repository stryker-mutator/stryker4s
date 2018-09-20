package stryker4s.run.report

import java.lang.System.lineSeparator

import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.model.{Detected, MutantRunResults, Undetected}
import stryker4s.run.report.mapper.MutantRunResultMapper

class ConsoleReporter extends MutantRunReporter with Logging with MutantRunResultMapper {

  override def report(runResults: MutantRunResults)(implicit config: Config): Unit = {
    val detected = runResults.results collect { case d: Detected => d }
    val detectedSize = detected.size

    val undetected = runResults.results collect { case u: Undetected => u }
    val undetectedSize = undetected.size

    val totalMutants = detectedSize + undetectedSize
    info(s"Mutation run finished! Took ${runResults.duration.toSeconds} seconds")
    info(s"Total mutants: $totalMutants, detected: $detectedSize, undetected: $undetectedSize")

    info(
      s"Undetected mutants:" + lineSeparator() +
        undetected
          .map { mrr =>
            val mutant = mrr.mutant

            val line = mutant.original.pos.startLine + 1
            val col = mutant.original.pos.startColumn + 1

            s"${mrr.fileSubPath}:$line:$col:" + lineSeparator() +
              s"\tfrom ${mutant.original} to ${mutant.mutated}" + lineSeparator()
          }
          .mkString(lineSeparator()))

    info(s"Mutation score: ${runResults.mutationScore}%")
  }
}
