package stryker4s.sbt.runner

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.Stream
import fs2.io.file
import fs2.io.file.Files
import fs2.io.process.{Process, ProcessBuilder}
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

import java.io.File
import java.net.{ConnectException, SocketException, StandardProtocolFamily}
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*

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
      javaHome: Option[File],
      classpath: Seq[Path],
      javaOpts: Seq[String],
      frameworks: Seq[Framework],
      testGroups: Seq[Tests.Group],
      id: TestRunnerId
  )(implicit config: Config, log: Logger): Resource[IO, ProcessTestRunner] =
    for {
      socketAddress <- getSocketAddress(id).toResource
      _ <- createProcess(javaHome, classpath, javaOpts, socketAddress, id)
      conn <- connectToProcess(socketAddress)
      _ <- setupTestRunner(conn, frameworks, testGroups).toResource
    } yield new ProcessTestRunner(conn)

  def createProcess(
      javaHome: Option[File],
      classpath: Seq[Path],
      javaOpts: Seq[String],
      socketAddress: GenSocketAddress,
      id: TestRunnerId
  )(implicit
      log: Logger,
      config: Config
  ): Resource[IO, Process[IO]] = {
    val classpathString = classpath.map(_.toString()).mkString(classPathSeparator)
    val javaBin = javaHome.fold("java")(h => (file.Path.fromNioPath(h.toPath()) / "bin" / "java").toString)
    val allArgs = List(
      "-Xmx4G",
      "-cp",
      classpathString
    ) ++ javaOpts ++ args(socketAddress)

    val logger = config.debug.logTestRunnerStdout
      .guard[Option]
      .as((m: String) => IO(log.debug(s"testrunner $id: $m")))

    for {
      args <-
        if (!sys.props.get("java.version").exists(_.startsWith("1.8.")))
          // Create a file with all arguments to pass to the java process
          // Sometimes the classpath and arguments is too long for the OS to handle, so we write it to a file and pass the file as argument
          Files[IO].tempFile
            .flatMap(argfile =>
              Stream
                .emits(allArgs)
                .intersperse(" ")
                .through(Files[IO].writeUtf8(argfile))
                .compile
                .resource
                .drain
                .as(List(s"@$argfile"))
            )
        // Argfiles are not supported in Java 8
        else Resource.pure[IO, List[String]](allArgs)
      _ <- IO(log.debug(s"Starting process '$javaBin ${args.mkString(" ")}'")).toResource
      process <- ProcessResource.fromProcessBuilder(
        ProcessBuilder(javaBin, args).withWorkingDirectory(config.baseDir),
        logger
      )
      _ <- IO(log.debug("Started process")).toResource
    } yield process
  }

  private def args(address: GenSocketAddress)(implicit config: Config): Seq[String] = {
    val mainClass = "stryker4s.sbt.testrunner.SbtTestRunnerMain"
    val sysProps = address match {
      case SocketAddress(_, port)  => s"-D${TestProcessProperties.port}=$port"
      case UnixSocketAddress(path) => s"-D${TestProcessProperties.unixSocketPath}=$path"
    }
    val debugArgs =
      if (config.debug.debugTestRunner)
        Seq("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:8000")
      else Seq.empty

    // Debug arguments must go before the main class
    debugArgs ++ Seq(sysProps, mainClass)
  }

  private def connectToProcess(
      socketAddress: GenSocketAddress
  )(implicit log: Logger): Resource[IO, TestRunnerConnection] = {
    val name = socketAddress match {
      case UnixSocketAddress(path) => path
      case SocketAddress(_, port)  => s"port :$port"
    }

    SocketTestRunnerConnection
      .create(socketAddress)
      .retryWithBackoff(
        6,
        0.2.seconds,
        delay => IO(log.debug(s"Could not connect to testprocess. Retrying after ${delay.toHumanReadable}..."))
      )
      .evalTap(_ => IO(log.debug(s"Connected to testprocess at $name")))
      .onFinalize(IO(log.debug(s"Closing test-runner at $name")))
  }

  def setupTestRunner(
      testProcess: TestRunnerConnection,
      frameworks: Seq[Framework],
      testGroups: Seq[Tests.Group]
  ): IO[Unit] = {
    val testContext = TestProcessContext(toApiTestGroups(frameworks, testGroups))

    testProcess.sendMessage(testContext).void
  }

  def getSocketAddress(id: TestRunnerId)(implicit config: Config): IO[GenSocketAddress] =
    unixSocketSupported.ifM(
      IO.pure(UnixSocketAddress((config.baseDir / "target" / s"stryker4s-$id.sock").toString)),
      IO.pure {
        val portStart = 13336
        SocketAddress(ipv4"127.0.0.1", Port.fromInt(portStart + id.value).get)
      }
    )

  /** Check if unix domain sockets are supported on this platform. If not, we fall back to TCP sockets.
    *
    * @see
    *   https://github.com/typelevel/fs2/blob/fdaae8959ad5d64fa0d30d78d9821897e7148bcf/io/jvm/src/main/scala/fs2/io/net/JdkUnixSocketsProvider.scala#L39
    */
  def unixSocketSupported: IO[Boolean] = IO(StandardProtocolFamily.values.size > 2)

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
      resource.handleErrorWith[A] {
        case _: ConnectException | _: SocketException if maxAttempts != 0 =>
          (onError(delay) *> IO.sleep(delay)).toResource *>
            retryWithBackoff(maxAttempts - 1, delay * 2, onError)
        case e => Resource.raiseError[IO, A, Throwable](new RuntimeException("Could not connect to testprocess", e))
      }
    }
  }

}
