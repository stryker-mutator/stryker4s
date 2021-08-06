package stryker4s.testutil.stubs

import better.files.File
import stryker4s.mutants.findmutants.SourceCollector

class TestSourceCollector(returns: Seq[File]) extends SourceCollector {
  override def collectFilesToMutate(): Seq[File] = returns
  override def filesToCopy: Seq[File] = returns
}
