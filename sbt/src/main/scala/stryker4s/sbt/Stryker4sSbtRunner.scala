package stryker4s.sbt

import sbt.{Extracted, _}
import stryker4s.run.Stryker4sRunner

/**
  * This Runner run Stryker mutations in a single SBT session
  *
  * @param state SBT project state (contains all the settings about the project)
  */
class Stryker4sSbtRunner(state: State) {

  private val extracted: Extracted = Project.extract(state)

  def run(): Unit = {
    new Stryker4sRunner().run()
  }

}
