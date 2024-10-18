package stryker4s.command

import cats.data.NonEmptyList
import cats.effect.{Deferred, IO, Resource}
import cats.syntax.either.*
import fs2.io.file.Path
import stryker4s.command.runner.ProcessTestRunner
import stryker4s.config.Config
import stryker4s.config.source.{CliConfigSource, ConfigSource}
import stryker4s.log.Logger
import stryker4s.model.CompilerErrMsg
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.tree.InstrumenterOptions
import stryker4s.run.process.ProcessRunner
import stryker4s.run.{Stryker4sRunner, TestRunner}

import scala.concurrent.duration.FiniteDuration

class Stryker4sCommandRunner(timeout: Deferred[IO, FiniteDuration], args: List[String])(implicit log: Logger)
    extends Stryker4sRunner {

  override def resolveTestRunners(
      tmpDir: Path
  )(implicit config: Config): Either[NonEmptyList[CompilerErrMsg], NonEmptyList[Resource[IO, TestRunner]]] = {
    val innerTestRunner =
      Resource.pure[IO, TestRunner](new ProcessTestRunner(config.testRunner, ProcessRunner(), tmpDir))

    val withTimeout = TestRunner.timeoutRunner(timeout, innerTestRunner)

    NonEmptyList.one(withTimeout).asRight
  }

  override def instrumenterOptions(implicit config: Config): InstrumenterOptions =
    InstrumenterOptions.sysContext(ActiveMutationContext.envVar)

  override def extraConfigSources: List[ConfigSource[IO]] = List(new CliConfigSource[IO](args))
}
