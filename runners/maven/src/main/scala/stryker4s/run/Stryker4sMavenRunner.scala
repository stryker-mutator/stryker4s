package stryker4s.run
import stryker4s.config.Config
import stryker4s.run.process.ProcessRunner

class Stryker4sMavenRunner extends Stryker4sRunner {
  override def resolveRunner()(implicit config: Config): MutantRunner =
    new MavenMutantRunner(ProcessRunner.resolveRunner())
}
