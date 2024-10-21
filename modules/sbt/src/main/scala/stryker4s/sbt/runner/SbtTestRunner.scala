package stryker4s.sbt.runner

import cats.effect.{Deferred, IO, Resource}
import com.comcast.ip4s.Port
import sbt.Tests
import sbt.testing.Framework
import stryker4s.config.Config
import stryker4s.log.Logger
import stryker4s.run.TestRunner

import java.nio.file.Path
import scala.concurrent.duration.FiniteDuration

object SbtTestRunner {
  def create(
      classpath: Seq[Path],
      javaOpts: Seq[String],
      frameworks: Seq[Framework],
      testGroups: Seq[Tests.Group],
      port: Port,
      timeout: Deferred[IO, FiniteDuration]
  )(implicit
      config: Config,
      log: Logger
  ): Resource[IO, TestRunner] = {
    // Timeout will be set by timeoutRunner after initialTestRun
    val innerTestRunner = ProcessTestRunner.newProcess(classpath, javaOpts, frameworks, testGroups, port)

    val withTimeout = TestRunner.timeoutRunner(timeout, innerTestRunner)

    val maybeWithMaxReuse = config.maxTestRunnerReuse.filter(_ > 0) match {
      case Some(reuses) => TestRunner.maxReuseTestRunner(reuses, withTimeout)
      case None         => withTimeout
    }

    val withRetryReuseAndTimeout = TestRunner.retryRunner(maybeWithMaxReuse)

    withRetryReuseAndTimeout
  }
}
