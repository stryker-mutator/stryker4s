package stryker4s.run

import grizzled.slf4j.Logging
import stryker4s.Stryker4s
import stryker4s.config.{CommandRunner, Config, ConfigReader}
import stryker4s.mutants.Mutator
import stryker4s.mutants.applymutants.{MatchBuilder, StatementTransformer}
import stryker4s.mutants.findmutants.{FileCollector, MutantFinder, MutantMatcher}
import stryker4s.run.process.{Command, ProcessMutantRunner, ProcessRunner}
import stryker4s.run.report.Reporter

import scala.meta.internal.tokenizers.PlatformTokenizerCache

object Stryker4sRunner extends App {
  val exitCode = new Stryker4sRunner().run()
  exit()

  private def exit(): Unit = {
    sys.exit(exitCode)
  }
}

class Stryker4sRunner extends Logging {

  implicit val config: Config = ConfigReader.readConfig()

  def run(): Int = {

    // Scalameta uses a cache file->tokens that exists at a process level
    // if one file changes between runs (in the same process, eg a single SBT session) could lead to an error, so
    // it is cleaned before it starts.
    PlatformTokenizerCache.megaCache.clear()

    new Stryker4s(
      new FileCollector,
      new Mutator(new MutantFinder(new MutantMatcher), new StatementTransformer, new MatchBuilder),
      resolveRunner(),
      new Reporter()
    ).run()
  }

  protected def resolveRunner()(implicit config: Config): MutantRunner = {
    config.testRunner match {
      case CommandRunner(command, args) =>
        new ProcessMutantRunner(Command(command, args), ProcessRunner.resolveRunner())
    }
  }
}
