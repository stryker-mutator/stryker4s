package stryker4s.sbt

import better.files.File
import sbt.{Extracted, LocalRootProject}
import stryker4s.Stryker4s
import stryker4s.config.Config
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher}
import stryker4s.sbt.Stryker4sPlugin.autoImport._
import sbt.Keys._
import sbt._

import stryker4s.run.report.Reporter

/**
  * This Runner run Stryker mutations in a single SBT session
  *
  * @param state SBT project state (contains all the settings about the project)
  */
class Stryker4sSbtRunner(state:State) {

  private val extracted: Extracted = Project.extract(state)

  private val baseDir = extracted.get(LocalRootProject / baseDirectory)

  // TODO: add more settings
  private val mutate = extracted.getOpt(strykerMutate).getOrElse(Seq("**/main/scala/**/*.scala"))
//  val logLevel = extracted.getOpt(strykerLogLevel)
//  val reports = extracted.getOpt(strykerReporters)

  // TODO: ConfigReader from SBT Settings (sbt.Extracted)
  // TODO: maybe decouple Config from PureConfig
  implicit val config = Config(
    mutate = mutate,
    baseDir = File(baseDir.getAbsolutePath),
    reporters = List(new SbtReporter())
  )

  def run(): Unit = {

    val stryker4s = new Stryker4s(
      new FileCollector,
      new Mutator(new MutantFinder(new MutantMatcher), new StatementTransformer, new MatchBuilder),
      new SbtMutantRunner(state),
      new Reporter()
    )

    stryker4s.run()

  }

}

