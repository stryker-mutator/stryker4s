package stryker4s.sbt.runner

import java.net.{InetAddress, Socket}

import scala.concurrent.duration._
import scala.sys.process.{Process, ProcessLogger}
import scala.util.control.NonFatal

import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.syntax.all._
import stryker4s.log.Logger
import sbt.Tests
import sbt.testing.{Framework => SbtFramework}
import stryker4s.api.testprocess._
import stryker4s.config.Config
import stryker4s.model.{MutantRunResult, _}
import stryker4s.run.TestRunner

class ProcessTestRunner(testProcess: TestRunnerConnection) extends TestRunner {

  override def runMutant(mutant: Mutant): IO[MutantRunResult] = {
    val message = StartTestRun(mutant.id)
    testProcess.sendMessage(message) map {
      case _: TestsSuccessful      => Survived(mutant)
      case _: TestsUnsuccessful    => Killed(mutant)
      case ErrorDuringTestRun(msg) => Killed(mutant, Some(msg))
      case _                       => Error(mutant)
    }
  }

  override def initialTestRun(): IO[Boolean] = {
    testProcess.sendMessage(StartInitialTestRun()) map {
      case _: TestsSuccessful => true
      case _                  => false
    }

  }

}

object ProcessTestRunner extends TestInterfaceMapper {
  private val classPathSeparator = java.io.File.pathSeparator

  def newProcess(
      classpath: Seq[String],
      javaOpts: Seq[String],
      frameworks: Seq[SbtFramework],
      testGroups: Seq[Tests.Group]
  )(implicit config: Config, log: Logger, timer: Timer[IO], cs: ContextShift[IO]): Resource[IO, ProcessTestRunner] = {
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
  )(implicit log: Logger, config: Config): Resource[IO, Process] = {
    val mainClass = "stryker4s.sbt.testrunner.SbtTestRunnerMain"
    val sysProps = s"-D${TestProcessProperties.port}=${socketConfig.port}"
    val args = Seq(sysProps, mainClass)
    val classpathString = classpath.mkString(classPathSeparator)
    val command = Seq("java", "-Xmx4G", "-cp", classpathString) ++ javaOpts ++ args

    for {
      _ <- Resource.liftF(IO(log.debug(s"Starting process ${command.mkString(" ")}")))
      startedProcess <- Resource.liftF(IO(scala.sys.process.Process(command, config.baseDir.toJava)))
      process <-
        Resource
          .make(IO(startedProcess.run(ProcessLogger(m => log.debug(s"testrunner: $m")))))(p => IO(p.destroy()))
          .evalTap(_ => IO(log.debug("Started process")))
    } yield process
  }

  private def connectToProcess(
      config: TestProcessConfig
  )(implicit timer: Timer[IO], log: Logger, cs: ContextShift[IO]): Resource[IO, TestRunnerConnection] = {
    // Sleep 0.5 seconds to let the process startup before attempting connection
    Resource.liftF(
      IO(log.debug("Creating socket"))
        .delayBy(0.5.seconds)
    ) *>
      Resource
        .make(
          retryWithBackoff(5, 0.5.seconds, log.info("Could not connect to testprocess. Retrying..."))(
            IO(new Socket(InetAddress.getLocalHost(), config.port))
          )
        )(s => IO(s.close()))
        .evalTap(_ => IO(log.debug("Created socket")))
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
