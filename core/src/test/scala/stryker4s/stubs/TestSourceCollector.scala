package stryker4s.stubs

import better.files.File
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.process.ProcessRunner

class TestSourceCollector(returns: Iterable[File]) extends SourceCollector {
  override def collectFilesToMutate(): Iterable[File] = returns
  override def filesToCopy(processRunner: ProcessRunner): Iterable[File] = returns
}
