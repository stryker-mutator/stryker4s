package stryker4s.testutil.stubs

import fs2.io.file.Path
import stryker4s.mutants.findmutants.SourceCollector

class TestSourceCollector(returns: Iterable[Path]) extends SourceCollector {
  override def collectFilesToMutate(): Iterable[Path] = returns
  override def filesToCopy: Iterable[Path] = returns
}
