package stryker4jvm.command

import cats.data.NonEmptyList
import cats.effect.{Deferred, IO, Resource}
import cats.syntax.either.*
import fs2.io.file.Path
import stryker4jvm.command.config.ProcessRunnerConfig
import stryker4jvm.command.runner.ProcessTestRunner
import stryker4jvm.config.Config
import stryker4jvm.core.model.InstrumenterOptions
import stryker4jvm.logging.FansiLogger
import stryker4jvm.model.CompilerErrMsg
import stryker4jvm.run.process.ProcessRunner
import stryker4jvm.run.{Stryker4jvmRunner, TestRunner}

import scala.concurrent.duration.FiniteDuration

class Stryker4jvmCommandRunner(processRunnerConfig: ProcessRunnerConfig, timeout: Deferred[IO, FiniteDuration])(implicit
    log: FansiLogger
) extends Stryker4jvmRunner {

  override def resolveTestRunners(
      tmpDir: Path
  )(implicit config: Config): Either[NonEmptyList[CompilerErrMsg], NonEmptyList[Resource[IO, TestRunner]]] = {
    val innerTestRunner =
      Resource.pure[IO, TestRunner](new ProcessTestRunner(processRunnerConfig.testRunner, ProcessRunner(), tmpDir))

    val withTimeout = TestRunner.timeoutRunner(timeout, innerTestRunner)

    NonEmptyList.one(withTimeout).asRight
  }

  override def instrumenterOptions(implicit config: Config): InstrumenterOptions =
    InstrumenterOptions.EnvVar
}
