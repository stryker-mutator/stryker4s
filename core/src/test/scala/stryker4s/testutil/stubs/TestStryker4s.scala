package stryker4s.testutil.stubs

import stryker4s.Stryker4s
import stryker4s.config.Config
import stryker4s.mutants.Mutator
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.run.MutantRunner
import stryker4s.run.report.Reporter

class TestStryker4s(jvmEnoughMemory: Boolean, fileCollector: SourceCollector, mutator: Mutator, runner: MutantRunner, reporter: Reporter)(
  implicit config: Config) extends Stryker4s(fileCollector, mutator, runner, reporter){

  override protected def jvmHasEnoughMemory: Boolean = jvmEnoughMemory

}
