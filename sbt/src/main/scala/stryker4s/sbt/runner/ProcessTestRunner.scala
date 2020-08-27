package stryker4s.sbt.runner

import java.net.InetAddress
import java.nio.file.Path
import java.net.Socket

import scala.concurrent.duration._
import scala.sys.process.{Process, ProcessLogger}
import scala.util.control.NonFatal

import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.implicits._
import grizzled.slf4j.Logging
import sbt.Tests
import sbt.testing.{Framework => SbtFramework}
import stryker4s.api.testprocess._
import stryker4s.model.{MutantRunResult, _}
import stryker4s.run.TestRunner

class ProcessTestRunner(testProcess: TestRunnerConnection) extends TestRunner with Logging {

  def runMutant(mutant: Mutant, path: Path): IO[MutantRunResult] = {
    val message = StartTestRun(mutant.id)
    testProcess.sendMessage(message) map {
      case _: TestsSuccessful      => Survived(mutant, path)
      case _: TestsUnsuccessful    => Killed(mutant, path)
      case ErrorDuringTestRun(msg) => Killed(mutant, path, Some(msg))
      case _                       => Error(mutant, path)
    }
  }

  def initialTestRun(): IO[Boolean] = {
    testProcess.sendMessage(StartInitialTestRun()) map {
      case _: TestsSuccessful => true
      case _                  => false
    }

  }

}

object ProcessTestRunner extends TestInterfaceMapper with Logging {
  private val classPathSeparator = java.io.File.pathSeparator

  def newProcess(
      classpath: Seq[String],
      javaOpts: Seq[String],
      frameworks: Seq[SbtFramework],
      testGroups: Seq[Tests.Group]
  )(implicit timer: Timer[IO], cs: ContextShift[IO]): Resource[IO, ProcessTestRunner] = {
    val socketConfig = TestProcessConfig(13337) // TODO: Don't hardcode socket port

    createProcess(classpath, javaOpts, socketConfig)
      .parZip(connectToProcess(socketConfig))
      .map(_._2)
      .evalTap(setupTestRunner(_, frameworks, testGroups))
      .map(new ProcessTestRunner(_))
  }

  def createProcess(
      classpath: Seq[String],
      javaOpts: Seq[String],
      socketConfig: TestProcessConfig
  ): Resource[IO, Process] = {
    val mainClass = "stryker4s.sbt.testrunner.SbtTestRunnerMain"
    val sysProps = s"-D${TestProcessProperties.port}=${socketConfig.port}"
    val args = Seq(sysProps, mainClass)
    val classpathString = classpath.mkString(classPathSeparator)
    val command = Seq("java", "-cp", classpathString) ++ javaOpts ++ args

    for {
      _ <- Resource.liftF(IO(debug(s"Starting process ${command.mkString(" ")}")))
      startedProcess <- Resource.liftF(IO(scala.sys.process.Process(command)))
      process <-
        Resource
          .make(IO(startedProcess.run(ProcessLogger(m => debug(s"testrunner: $m")))))(p => IO(p.destroy()))
          .evalTap(_ => IO(debug("Started process")))
    } yield process
  }

  private def connectToProcess(
      config: TestProcessConfig
  )(implicit timer: Timer[IO], cs: ContextShift[IO]): Resource[IO, TestRunnerConnection] = {
    // Sleep 0.5 seconds to let the process startup before attempting connection
    Resource.liftF(
      IO.sleep(0.5.seconds) *>
        IO(debug("Creating socket"))
    ) *>
      Resource
        .make(
          retryWithBackoff(5, 0.5.seconds, info("Could not connect to testprocess. Retrying..."))(
            IO(new Socket(InetAddress.getLocalHost(), config.port))
          )
        )(s => IO(s.close()))
        .evalTap(_ => IO(debug("Created socket")))
        .flatMap(TestRunnerConnection.create(_))
  }

  def setupTestRunner(
      testProcess: TestRunnerConnection,
      frameworks: Seq[SbtFramework],
      testGroups: Seq[Tests.Group]
  ): IO[Unit] = {
    val apiTestGroups = TestProcessContext(toApiTestGroups(frameworks, testGroups))

    testProcess.sendMessage(SetupTestContext(apiTestGroups)).void
  }

  def retryWithBackoff[T](maxAttempts: Int, delay: FiniteDuration, onError: => Unit)(
      f: IO[T]
  )(implicit timer: Timer[IO]): IO[T] = {
    val retriableWithOnError = (NonFatal.apply(_)).compose((t: Throwable) => { onError; t })

    fs2.Stream
      // Exponential backoff
      .retry(f, delay, d => d * 2, maxAttempts, retriableWithOnError)
      .compile
      .lastOrError
  }
}
