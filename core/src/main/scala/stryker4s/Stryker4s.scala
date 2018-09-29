package stryker4s

import stryker4s.config.Config
import stryker4s.mutants.Mutator
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.MutantRunner
import stryker4s.run.report.Reporter

class Stryker4s(fileCollector: SourceCollector,
                mutator: Mutator,
                runner: MutantRunner,
                reporter: Reporter)(implicit config: Config) {

  def run(): Unit = {
    val files = fileCollector.collectFiles()
    val mutatedFiles = mutator.mutate(files)
    val runResults = runner(mutatedFiles, fileCollector)
    reporter.report(runResults)
  }
}
