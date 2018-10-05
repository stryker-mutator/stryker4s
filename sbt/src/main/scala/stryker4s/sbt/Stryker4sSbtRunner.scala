package stryker4s.sbt
import java.nio.file.Path
import java.util.concurrent.TimeUnit

import grizzled.slf4j.Logging
import better.files.File
import sbt.{Extracted, LocalRootProject}
import stryker4s.Stryker4s
import stryker4s.config.Config
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher}
import stryker4s.sbt.Stryker4sPlugin.autoImport._
import sbt.Keys._
import stryker4s.extensions.score.MutationScoreCalculator
import stryker4s.model._
import stryker4s.run.MutantRunner
import stryker4s.run.report.Reporter
import stryker4s.extensions.FileExtensions._

import scala.concurrent.duration.{Duration, MILLISECONDS}

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
      new SbtMutantRunner(extracted),
      new Reporter()
    )

    stryker4s.run()

  }

}

class SbtMutantRunner(extracted:Extracted)(implicit config:Config) extends MutantRunner with MutationScoreCalculator with Logging {

  def apply(files: Iterable[MutatedFile]): MutantRunResults = {

    println(s"RUNNING TEST!!!! FOR ${files.size}")

    val startTime = System.currentTimeMillis()
    val tmpDir = File.newTemporaryDirectory("stryker4s-")
    debug("Using temp directory: " + tmpDir)

    config.baseDir.copyTo(tmpDir)

    // Overwrite files to mutated files
    files foreach {
      case MutatedFile(file, tree, _) =>
        val subPath = file.relativePath
        val filePath = tmpDir / subPath.toString
        filePath.overwrite(tree.syntax)
    }

    val totalMutants = files.flatMap(_.mutants).size

    val runResults = for {
      mutatedFile <- files
      subPath = mutatedFile.fileOrigin.relativePath
      mutant <- mutatedFile.mutants
    } yield {
      val result = runMutant(mutant, tmpDir, subPath)
      val id = mutant.id
      info(
        s"Finished mutation run $id/$totalMutants (${((id / totalMutants.toDouble) * 100).round}%)")
      result
    }

    val duration = Duration(System.currentTimeMillis() - startTime, MILLISECONDS)
    val detected = runResults collect { case d: Detected => d }

    MutantRunResults(runResults, calculateMutationScore(totalMutants, detected.size), duration)

  }

  private[this] def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult = {

    println(mutant)
    println(workingDir)
    println(subPath)

    // coupled to Process
    Killed(0, mutant, subPath)
  }

}