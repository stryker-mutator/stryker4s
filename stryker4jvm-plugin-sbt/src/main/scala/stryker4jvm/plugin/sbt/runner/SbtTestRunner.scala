package stryker4jvm.plugin.sbt.runner

import cats.effect.{Deferred, IO, Resource}
import com.comcast.ip4s.Port
import sbt.Tests
import sbt.testing.Framework
import stryker4jvm.config.Config
import stryker4jvm.core.logging.Logger
import stryker4jvm.run.TestRunner

import scala.concurrent.duration.FiniteDuration

object SbtTestRunner {
  def create(
      classpath: Seq[String],
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
