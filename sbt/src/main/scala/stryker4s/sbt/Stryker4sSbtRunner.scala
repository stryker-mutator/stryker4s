package stryker4s.sbt
import java.util.concurrent.TimeUnit

import better.files.File
import sbt.{Extracted, LocalRootProject}
import stryker4s.Stryker4s
import stryker4s.config.Config
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher}
import stryker4s.sbt.Stryker4sPlugin.autoImport._
import sbt.Keys._
import stryker4s.model.{MutantRunResults, MutatedFile}
import stryker4s.run.MutantRunner
import stryker4s.run.report.Reporter

import scala.concurrent.duration.Duration

class Stryker4sSbtRunner(extracted:Extracted) {

  val baseDir = extracted.get(LocalRootProject / baseDirectory)

  val mutate = extracted.getOpt(strykerMutate).getOrElse(Seq("**/main/scala/**/*.scala"))
//  val logLevel = extracted.getOpt(strykerLogLevel)
//  val reports = extracted.getOpt(strykerReporters)

  // TODO: ConfigReader from SBT Settings (sbt.Extracted)
  // TODO: decouple Config from PureConfig
  implicit val config = Config(
    mutate = mutate,
    baseDir = File(baseDir.getAbsolutePath)
  )

  def run(): Unit = {

    val stryker4s = new Stryker4s(
      new FileCollector,
      new Mutator(new MutantFinder(new MutantMatcher), new StatementTransformer, new MatchBuilder),
      new SbtMutantRunner,
      new Reporter()
    )

    stryker4s.run()

  }

}

class SbtMutantRunner extends MutantRunner {
  def apply(files: Iterable[MutatedFile]): MutantRunResults = {

    println(s"RUNNING TEST!!!! FOR ${files.size}")



    // temp
    // copy
    //override
    // call task test
    // get results
    MutantRunResults(Nil, 100, Duration(1, TimeUnit.SECONDS))
  }
}