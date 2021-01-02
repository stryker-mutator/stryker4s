package stryker4s.command.runner

import better.files.File
import cats.effect.{ContextShift, IO, Resource, Timer}
import stryker4s.config.Config
import stryker4s.log.Logger
import stryker4s.mutants.findmutants.SourceCollector
import stryker4s.report.Reporter
import stryker4s.run.process.{Command, ProcessRunner}
import stryker4s.run.{MutantRunner, TestRunner}

class ProcessMutantRunner(
    command: Command,
    processRunner: ProcessRunner,
    sourceCollector: SourceCollector,
    reporter: Reporter
)(implicit config: Config, log: Logger, timer: Timer[IO], cs: ContextShift[IO])
    extends MutantRunner(sourceCollector, reporter) {

  override def initializeTestRunner(tmpDir: File): Resource[IO, TestRunner] =
    Resource.pure[IO, TestRunner](new ProcessTestRunner(command, processRunner, tmpDir))

}
