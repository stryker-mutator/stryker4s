package stryker4s.sbt.runner

import java.net.{InetAddress, Socket}

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.sys.process.Process
import scala.util.control.NonFatal

import cats.effect.{IO, Resource}
import cats.syntax.all._
import sbt.Tests
import sbt.testing.{Framework => SbtFramework}
import stryker4s.api.testprocess._
import stryker4s.config.Config
import stryker4s.log.Logger
import stryker4s.model.{MutantRunResult, _}
import stryker4s.run.process.ProcessResource
import stryker4s.run.{InitialTestRunResult, TestRunner}

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

  /** Initial test-run is done twice. This allows us to collect coverage data while filtering out 'static' mutants.
    * Mutants are considered static if they are initialized only once. This means the value cannot be changed using mutation switching.
    * For example, a `val a = 2` inside an `object` is considered static.
    *
    * In the first initial test-run, coverage data is collected. When running the second time any static mutants will not have coverage because their code will not be executed a second time, so we can filter those out.
    * See also https://github.com/stryker-mutator/stryker4s/pull/565#issuecomment-688438699
    */
  override def initialTestRun(): IO[InitialTestRunResult] = {
    val initialTestRun = testProcess.sendMessage(StartInitialTestRun())

    initialTestRun
      .map2(initialTestRun) {
        case (firstRun: CoverageTestRunResult, secondRun: CoverageTestRunResult) =>
          Right(
            InitialTestRunCoverageReport(
              firstRun.isSuccessful && secondRun.isSuccessful,
              firstRun.coverageReport.asScala.toMap.mapValues(_.toSeq),
              secondRun.coverageReport.asScala.toMap.mapValues(_.toSeq)
            )
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
      testGroups: Seq[Tests.Group]
  )(implicit config: Config, log: Logger): Resource[IO, ProcessTestRunner] = {
    val socketConfig = TestProcessConfig(13337) // TODO: Don't hardcode socket port

    (createProcess(classpath, javaOpts, socketConfig), connectToProcess(socketConfig))
      .parMapN({ case (_, c) => c })
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

    Resource.eval(IO(log.debug(s"Starting process ${command.mkString(" ")}"))) *>
      ProcessResource
        .fromProcessBuilder(Process(command, config.baseDir.toJava))(m => log.debug(s"testrunner: $m"))
        .evalTap(_ => IO(log.debug("Started process")))

  }

  private def connectToProcess(
      config: TestProcessConfig
  )(implicit log: Logger): Resource[IO, TestRunnerConnection] = {
    // Sleep 0.5 seconds to let the process startup before attempting connection
    Resource.eval(
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

  def retryWithBackoff[T](maxAttempts: Int, delay: FiniteDuration, onError: => Unit)(f: IO[T]): IO[T] = {
    val retriableWithOnError = (NonFatal.apply(_)).compose((t: Throwable) => { onError; t })

    fs2.Stream
      // Exponential backoff
      .retry(f, delay, d => d * 2, maxAttempts, retriableWithOnError)
      .compile
      .lastOrError
  }
}
