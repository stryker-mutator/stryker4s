package stryker4s.sbt.runner

import scala.concurrent.duration._

import cats.effect.concurrent.Deferred
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
    // Timeout will be set by timeoutRunner after initialTestRun
    // The timeout Deferred is wrapped around all other Resources so it doesn't get recreated on errors
    Resource.liftF(Deferred[IO, FiniteDuration]).flatMap { timeout =>
      // Resource.liftF(Ref.of[IO, Deferred[IO, FiniteDuration]](Deferred[IO, FiniteDuration])).flatMap { timeout =>
      val inner: Resource[IO, TestRunner] = ProcessTestRunner.newProcess(classpath, javaOpts, frameworks, testGroups)

      val withTimeout: Resource[IO, TestRunner] =
        TestRunner.timeoutRunner(timeout, inner)

      val withRetryAndTimeout = TestRunner.retryRunner(withTimeout)

      withRetryAndTimeout
    }
  }
}
