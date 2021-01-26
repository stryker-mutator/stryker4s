package stryker4s.sbt.runner

import cats.effect.{ContextShift, IO, Resource, Timer}
import sbt.Tests
import sbt.testing.Framework
import stryker4s.config.Config
import stryker4s.log.Logger
import stryker4s.run.TestRunner

object SbtTestRunner {
  def create(
      classpath: Seq[String],
      javaOpts: Seq[String],
      frameworks: Seq[Framework],
      testGroups: Seq[Tests.Group],
      port: Int
  )(implicit
      config: Config,
      log: Logger,
      timer: Timer[IO],
      cs: ContextShift[IO]
  ): Resource[IO, TestRunner] = {
    // Timeout will be set by timeoutRunner after initialTestRun
    // The timeout Deferred is wrapped around all other Resources so it doesn't get recreated on errors
    // Resource.liftF(Deferred[IO, FiniteDuration]).flatMap { timeout =>
    val innerTestRunner = ProcessTestRunner.newProcess(classpath, javaOpts, frameworks, testGroups, port)

    // TODO: Set timeout in all testrunners
    // val withTimeout = TestRunner.timeoutRunner(timeout, innerTestRunner)

    val maybeWithMaxReuse = config.maxTestRunnerReuse.filter(_ > 0) match {
      case Some(reuses) => TestRunner.maxReuseTestRunner(reuses, innerTestRunner)
      case None         => innerTestRunner
    }

    val withRetryReuseAndTimeout = TestRunner.retryRunner(maybeWithMaxReuse)

    withRetryReuseAndTimeout
    // }
  }
}
