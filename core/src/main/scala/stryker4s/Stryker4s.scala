package stryker4s

import stryker4s.config.Config
import stryker4s.mutants.Mutator
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.MutantRunner
import stryker4s.run.report.MutantRunReporter

class Stryker4s(fileCollector: SourceCollector, mutator: Mutator, runner: MutantRunner, reporter: MutantRunReporter)(
    implicit config: Config) {

  def run() = {
    val files = fileCollector.collectFiles()
    val mutatedFiles = mutator.mutate(files)
    val runResults = runner(mutatedFiles)
    reporter.report(runResults)
  }
}
