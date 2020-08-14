package stryker4s.sbt.runner

import cats.effect.Resource
import cats.effect.IO
import stryker4s.run.TestRunner
import cats.effect.Timer
import cats.effect.ContextShift
import scala.concurrent.duration._
import sbt.testing.Framework
import sbt.Tests

object SbtTestRunner {
  def create(classpath: Seq[String], frameworks: Seq[Framework], testGroups: Seq[Tests.Group])(implicit
      timer: Timer[IO],
      cs: ContextShift[IO]
  ): Resource[IO, TestRunner] = {
    val inner: Resource[IO, TestRunner] = ProcessTestRunner.newProcess(classpath, frameworks, testGroups)

    val withTimeout: Resource[IO, TestRunner] = inner
      // TODO: Properly set timeout based on initial testrun
      .map(TestRunner.timeoutRunner(2.minutes, _))

    val withRetryAndTimeout = TestRunner.retryRunner(withTimeout)

    withRetryAndTimeout
  }
}
