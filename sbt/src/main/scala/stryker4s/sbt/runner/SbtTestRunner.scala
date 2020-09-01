package stryker4s.sbt.runner

import scala.concurrent.duration._

import cats.effect.{ContextShift, IO, Resource, Timer}
import sbt.Tests
import sbt.testing.Framework
import stryker4s.config.Config
import stryker4s.run.TestRunner

object SbtTestRunner {
  def create(classpath: Seq[String], javaOpts: Seq[String], frameworks: Seq[Framework], testGroups: Seq[Tests.Group])(
      implicit
      config: Config,
      timer: Timer[IO],
      cs: ContextShift[IO]
  ): Resource[IO, TestRunner] = {
    val inner: Resource[IO, TestRunner] = ProcessTestRunner.newProcess(classpath, javaOpts, frameworks, testGroups)

    // TODO: Properly set timeout based on initial testrun
    val withTimeout: Resource[IO, TestRunner] =
      TestRunner.timeoutRunner(2.minutes, inner)

    val withRetryAndTimeout = TestRunner.retryRunner(withTimeout)

    withRetryAndTimeout
  }
}
