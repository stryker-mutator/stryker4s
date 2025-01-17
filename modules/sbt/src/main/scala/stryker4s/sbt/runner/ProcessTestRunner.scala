package stryker4s.sbt.runner

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import com.comcast.ip4s.{Host, Port, SocketAddress}
import fs2.io.net.Network
import mutationtesting.{MutantResult, MutantStatus}
import sbt.Tests
import sbt.testing.Framework
import stryker4s.config.Config
import stryker4s.extension.DurationExtensions.*
import stryker4s.log.Logger
import stryker4s.model.*
import stryker4s.run.TestRunner
import stryker4s.run.process.ProcessResource
import stryker4s.testrunner.api.*

import java.net.ConnectException
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*
import scala.sys.process.Process

class ProcessTestRunner(testProcess: TestRunnerConnection) extends TestRunner {

  override def runMutant(mutant: MutantWithId, testsToRun: Seq[TestFile]): IO[MutantResult] = {
    val message = StartTestRun.of(mutant.id, testsToRun.map(_.fullyQualifiedName))
    val coveredBy = testsToRun.flatMap(_.definitions).map(_.id).some

    testProcess.sendMessage(message).map {
      case TestsSuccessful(testsCompleted) =>
        mutant.toMutantResult(MutantStatus.Survived, testsCompleted = testsCompleted.some, coveredBy = coveredBy)
      case TestsUnsuccessful(testsCompleted, failedTests) =>
        mutant.toMutantResult(
          MutantStatus.Killed,
          testsCompleted = testsCompleted.some,
          coveredBy = coveredBy,
          killedBy = extractKilledBy(testsToRun, failedTests).some,
          statusReason = extractStatusReason(failedTests)
        )
      case ErrorDuringTestRun(msg) =>
        mutant.toMutantResult(MutantStatus.Killed, statusReason = msg.some, coveredBy = coveredBy)
      case _ => mutant.toMutantResult(MutantStatus.RuntimeError, coveredBy = coveredBy)
    }
  }

  /** Map failed test names to their corresponding test ids.
    */
  private def extractKilledBy(
      testsToRun: Seq[TestFile],
      failedTests: Seq[FailedTestDefinition]
  ): Seq[TestDefinitionId] =
    failedTests.flatMap { failedTest =>
      testsToRun
        .find(t => failedTest.fullyQualifiedName.contains(t.fullyQualifiedName))
        .flatMap(_.definitions.find(_.name == failedTest.name).map(_.id))
    }

  /** Extract the status reason from the failed tests into a single string (or None).
    */
  private def extractStatusReason(failedTests: Seq[FailedTestDefinition]): Option[String] =
    NonEmptyList
      .fromList(
        failedTests
          .flatMap(test =>
            test.message.map(msg =>
              // TODO: change `.plainText` to `.render` when mutation-testing-elements supports rendering ansi-codes https://github.com/stryker-mutator/mutation-testing-elements/issues/2925
              fansi.Str(test.name, ": ", msg).plainText
            )
          )
          .toList
      )
      .map(_.mkString_("\n\n"))

  /** Initial test-run is done twice. This allows us to collect coverage data while filtering out 'static' mutants.
    * Mutants are considered static if they are initialized only once. This means the value cannot be changed using
    * mutation switching. For example, a `val a = 2` inside an `object` is considered static.
    *
    * In the first initial test-run, coverage data is collected. When running the second time any static mutants will
    * not have coverage because their code will not be executed a second time, so we can filter those out.
    *
    * @see
    *   https://github.com/stryker-mutator/stryker4s/pull/565#issuecomment-688438699
    */
  override def initialTestRun(): IO[InitialTestRunResult] = {
    val initialTestRun = testProcess.sendMessage(StartInitialTestRun())

    initialTestRun.map2(initialTestRun) {
      case (firstRun: CoverageTestRunResult, secondRun: CoverageTestRunResult) =>
        val averageDuration =
          FiniteDuration((firstRun.durationNanos + secondRun.durationNanos) / 2, TimeUnit.NANOSECONDS)

        InitialTestRunCoverageReport(
          firstRun.isSuccessful && secondRun.isSuccessful,
          CoverageReport(firstRun.getCoverageTestNameMap),
          CoverageReport(secondRun.getCoverageTestNameMap),
          averageDuration,
          firstRun.getCoverageTestNameMap.testNameIds.values.toSeq
        )
      case x => throw new MatchError(x)
    }
  }
}

object ProcessTestRunner extends TestInterfaceMapper {
  private val classPathSeparator = java.io.File.pathSeparator

  def newProcess(
      classpath: Seq[Path],
      javaOpts: Seq[String],
      frameworks: Seq[Framework],
      testGroups: Seq[Tests.Group],
      port: Port
  )(implicit config: Config, log: Logger): Resource[IO, ProcessTestRunner] =
    (createProcess(classpath, javaOpts, port) *> connectToProcess(port))
      .evalTap(setupTestRunner(_, frameworks, testGroups))
      .map(new ProcessTestRunner(_))

  def createProcess(
      classpath: Seq[Path],
      javaOpts: Seq[String],
      port: Port
  )(implicit log: Logger, config: Config): Resource[IO, Process] = {
    val classpathString = classpath.map(_.toString()).mkString(classPathSeparator)
    val command = Seq("java", "-Xmx4G", "-cp", classpathString) ++ javaOpts ++ args(port)

    val logger: String => Unit =
      if (config.debug.logTestRunnerStdout) m => log.debug(s"testrunner $port: $m")
      else _ => ()

    ProcessResource
      .fromProcessBuilder(Process(command, config.baseDir.toNioPath.toFile()))(logger)
      .preAllocate(IO(log.debug(s"Starting process '${command.mkString(" ")}'")))
      .evalTap(_ => IO(log.debug("Started process")))
  }

  private def args(port: Port)(implicit config: Config): Seq[String] = {
    val mainClass = "stryker4s.sbt.testrunner.SbtTestRunnerMain"
    val sysProps = s"-D${TestProcessProperties.port}=$port"
    val debugArgs =
      if (config.debug.debugTestRunner)
        Seq("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:8000")
      else Seq.empty

    // Debug arguments must go before the main class
    debugArgs ++ Seq(sysProps, mainClass)
  }

  private def connectToProcess(port: Port)(implicit log: Logger): Resource[IO, TestRunnerConnection] = {
    val socketAddress = SocketAddress(Host.fromString("127.0.0.1").get, port)

    Network[IO]
      .client(socketAddress)
      .map(new SocketTestRunnerConnection(_))
      .retryWithBackoff(
        6,
        0.2.seconds,
        delay => IO(log.debug(s"Could not connect to testprocess. Retrying after ${delay.toHumanReadable}..."))
      )
      .evalTap(_ => IO(log.debug(s"Connected to testprocess on port $port")))
      .onFinalize(IO(log.debug(s"Closing test-runner on port $port")))
  }

  def setupTestRunner(
      testProcess: TestRunnerConnection,
      frameworks: Seq[Framework],
      testGroups: Seq[Tests.Group]
  ): IO[Unit] = {
    val testContext = TestProcessContext(toApiTestGroups(frameworks, testGroups))

    testProcess.sendMessage(testContext).void
  }

  implicit final class ResourceOps[A](val resource: Resource[IO, A]) extends AnyVal {

    /** Retry creating the resource, with an increasing (doubling) backoff until the resource is created, or fails
      * @param maxRetries
      *   times.
      */
    final def retryWithBackoff(
        maxAttempts: Int,
        delay: FiniteDuration,
        onError: FiniteDuration => IO[Unit]
    ): Resource[IO, A] = {
      resource.handleErrorWith[A, Throwable] {
        case _: ConnectException if maxAttempts != 0 =>
          (onError(delay) *> IO.sleep(delay)).toResource *>
            retryWithBackoff(maxAttempts - 1, delay * 2, onError)
        case _ => Resource.raiseError[IO, A, Throwable](new RuntimeException("Could not connect to testprocess"))
      }
    }
  }

}
