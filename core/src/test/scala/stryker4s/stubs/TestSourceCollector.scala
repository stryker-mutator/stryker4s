package stryker4s.stubs

import better.files.File
import stryker4s.mutants.findmutants.SourceCollector

class TestSourceCollector(returns: Iterable[File]) extends SourceCollector {
  override def collectFiles(): Iterable[File] = returns
}
