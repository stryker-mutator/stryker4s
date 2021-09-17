package stryker4s.sbt.runner

import cats.effect.{IO, Resource}
import cats.syntax.apply._
import cats.syntax.parallel._
import sbt.Tests
import sbt.testing.{Framework => SbtFramework}
import stryker4s.api.testprocess._
import stryker4s.config.Config
import stryker4s.log.Logger
import stryker4s.model.{MutantRunResult, _}
import stryker4s.run.TestRunner
import stryker4s.run.process.ProcessResource

import java.net.{InetAddress, Socket}
import scala.concurrent.duration._
import scala.sys.process.Process
import scala.util.control.NonFatal

class ProcessTestRunner(testProcess: TestRunnerConnection) extends TestRunner {

  override def runMutant(mutant: Mutant): IO[MutantRunResult] = {
    val message = StartTestRun(mutant.id)
    testProcess.sendMessage(message).map {
      case _: TestsSuccessful         => Survived(mutant)
      case _: TestsUnsuccessful       => Killed(mutant)
      case ErrorDuringTestRun(msg) => Killed(mutant, Some(msg))
      case _                          => Error(mutant)
    }
  }

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
    val initialTestRun = testProcess.sendMessage(StartInitialTestRun()).timed

    initialTestRun
      .map2(initialTestRun) {
        case ((firstDuration, firstRun: CoverageTestRunResult), (secondDuration, secondRun: CoverageTestRunResult)) =>
          val averageDuration = (firstDuration + secondDuration) / 2
          InitialTestRunCoverageReport(
            firstRun.isSuccessful && secondRun.isSuccessful,
            firstRun.coverageReport.mapValues(_.fingerprint),
            secondRun.coverageReport.mapValues(_.fingerprint),
            averageDuration
          )
        case x => throw new MatchError(x)
      }
  }
}

object ProcessTestRunner extends TestInterfaceMapper {
  private val classPathSeparator = java.io.File.pathSeparator

  def newProcess(
      classpath: Seq[String],
      javaOpts: Seq[String],
      frameworks: Seq[SbtFramework],
      testGroups: Seq[Tests.Group],
      port: Int
  )(implicit config: Config, log: Logger): Resource[IO, ProcessTestRunner] =
    (createProcess(classpath, javaOpts, port), connectToProcess(port))
      .parMapN { case (_, c) => c }
      .evalTap(setupTestRunner(_, frameworks, testGroups))
      .map(new ProcessTestRunner(_))

  def createProcess(
      classpath: Seq[String],
      javaOpts: Seq[String],
      port: Int
  )(implicit log: Logger, config: Config): Resource[IO, Process] = {
    val mainClass = "stryker4s.sbt.testrunner.SbtTestRunnerMain"
    val sysProps = s"-D${TestProcessProperties.port}=$port"
    val args = Seq(sysProps, mainClass)
    val classpathString = classpath.mkString(classPathSeparator)
    val command = Seq("java", "-Xmx4G", "-cp", classpathString) ++ javaOpts ++ args

    Resource.eval(IO(log.debug(s"Starting process ${command.mkString(" ")}"))) *>
      ProcessResource
        .fromProcessBuilder(Process(command, config.baseDir.toNioPath.toFile()))(m =>
          log.debug(s"testrunner $port: $m")
        )
        .evalTap(_ => IO(log.debug("Started process")))

  }

  private def connectToProcess(port: Int)(implicit log: Logger): Resource[IO, TestRunnerConnection] = {
    Resource
      .make(
        retryWithBackoff(6, 0.2.seconds, log.debug("Could not connect to testprocess. Retrying..."))(
          IO(new Socket(InetAddress.getLoopbackAddress(), port))
        )
      )(s => IO(log.debug(s"Closing test-runner on port $port")) *> IO(s.close()))
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

  def retryWithBackoff[T](maxAttempts: Int, delay: FiniteDuration, onError: => Unit)(f: IO[T]): IO[T] = {
    val retriableWithOnError = (NonFatal.apply(_)).compose((t: Throwable) => { onError; t })

    fs2.Stream
      // Linear backoff
      .retry(f, delay, d => d + d, maxAttempts, retriableWithOnError)
      .compile
      .lastOrError
  }

}
