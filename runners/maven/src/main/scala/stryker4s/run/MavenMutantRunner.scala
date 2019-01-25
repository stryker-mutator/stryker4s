package stryker4s.run
import java.nio.file.Path

import better.files.File
import stryker4s.config.Config
import stryker4s.model.{Mutant, MutantRunResult}
import stryker4s.run.process.ProcessRunner

class MavenMutantRunner(processRunner: ProcessRunner)(implicit config: Config) extends MutantRunner(processRunner) {

  override def runInitialTest(workingDir: File): Boolean = ???

  override def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult = ???
}
