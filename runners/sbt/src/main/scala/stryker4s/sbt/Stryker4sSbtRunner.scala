package stryker4s.sbt

import sbt._
import stryker4s.config.Config
import stryker4s.run.process.ProcessRunner
import stryker4s.run.{MutantRunner, Stryker4sRunner}

/**
  * This Runner run Stryker mutations in a single SBT session
  *
  * @param state SBT project state (contains all the settings about the project)
  */
class Stryker4sSbtRunner(state: State) extends Stryker4sRunner {

  override def resolveRunner()(implicit config: Config): MutantRunner =
    new SbtMutantRunner(state, ProcessRunner.resolveRunner())

}
