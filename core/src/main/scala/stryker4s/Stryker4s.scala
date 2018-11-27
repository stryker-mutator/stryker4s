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
    val filesToMutate = fileCollector.collectFilesToMutate()
    val mutatedFiles = mutator.mutate(filesToMutate)
    val runResults = runner(mutatedFiles, fileCollector)
    reporter.report(runResults)
    val exitCode = reporter.determineExitCode(runResults)
    sys.exit(exitCode)
  }
}
