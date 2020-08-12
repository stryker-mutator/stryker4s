package stryker4s.sbt.runner

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.InetAddress
import java.nio.file.Path

import scala.concurrent.duration._
import scala.sys.process.{Process, ProcessLogger}
import scala.tools.nsc.io.Socket
import scala.util.control.NonFatal

import cats.effect.{IO, Timer}
import grizzled.slf4j.Logging
import sbt.testing.{Framework => SbtFramework}
import sbt.{TestDefinition => SbtTestDefinition, TestFramework => SbtTestFramework, Tests}
import stryker4s.api.testprocess._
import stryker4s.extension.exception.MutationRunFailedException
import stryker4s.model.{MutantRunResult, _}

class ProcessManager(testProcess: TestProcess) extends Logging {

  def runMutant(mutant: Mutant, path: Path): MutantRunResult = {
    val message = StartTestRun(mutant.id)
    testProcess.sendMessage(message) match {
      case _: TestsSuccessful    => Survived(mutant, path)
      case _: TestsUnsuccessful  => Killed(mutant, path)
      case _: ErrorDuringTestRun => Killed(mutant, path)
      case _                     => Error(mutant, path)
    }
  }

  def initialTestRun(frameworks: Seq[SbtFramework], testGroups: Seq[Tests.Group]): Boolean = {
    val apiTestGroups = TestProcessContext(toApiTestGroups(frameworks, testGroups))

    testProcess.sendMessage(SetupTestContext(apiTestGroups))

    testProcess.sendMessage(StartInitialTestRun()) match {
      case _: TestsSuccessful => true
      case _                  => false
    }

  }
  private def toApiTestGroups(frameworks: Seq[SbtFramework], sbtTestGroups: Seq[Tests.Group]): Array[TestGroup] = {
    val mapped = testMap(frameworks, sbtTestGroups.flatMap(_.tests))
    mapped
      .map({
        case (framework, tests) =>
          val taskDefs: Array[TaskDefinition] = tests.map(toTaskDefinition).toArray
          val runnerOptions = RunnerOptions(Array.empty, Array.empty)
          TestGroup(framework.getClass.getCanonicalName(), taskDefs, runnerOptions)
      })
      .toArray
  }

  /** From https://github.com/sbt/sbt/blob/develop/testing/src/main/scala/sbt/TestFramework.scala
    */
  private def testMap(
      frameworks: Seq[SbtFramework],
      tests: Seq[SbtTestDefinition]
  ): Map[SbtFramework, Set[SbtTestDefinition]] = {
    import scala.collection.mutable.{HashMap, HashSet, Set}
    val map = new HashMap[SbtFramework, Set[SbtTestDefinition]]
    def assignTest(test: SbtTestDefinition): Unit = {
      def isTestForFramework(framework: SbtFramework) =
        SbtTestFramework.getFingerprints(framework).exists { t =>
          SbtTestFramework.matches(t, test.fingerprint)
        }
      for (framework <- frameworks.find(isTestForFramework))
        map.getOrElseUpdate(framework, new HashSet[SbtTestDefinition]) += test
    }
    if (frameworks.nonEmpty)
      for (test <- tests) assignTest(test)
    map.toMap.mapValues(_.toSet)
  }

  private def toTaskDefinition(td: SbtTestDefinition): TaskDefinition = {
    val fingerprint = toFingerprint(td.fingerprint)
    val selectors = td.selectors.map(toSelector)
    TaskDefinition(td.name, fingerprint, td.explicitlySpecified, selectors)
  }

  private def toFingerprint(fp: sbt.testing.Fingerprint): Fingerprint =
    fp match {
      case a: sbt.testing.AnnotatedFingerprint => AnnotatedFingerprint(a.isModule(), a.annotationName())
      case s: sbt.testing.SubclassFingerprint =>
        SubclassFingerprint(s.isModule(), s.superclassName(), s.requireNoArgConstructor())
    }

  private def toSelector(s: sbt.testing.Selector): Selector =
    s match {
      case a: sbt.testing.NestedSuiteSelector  => NestedSuiteSelector(a.suiteId())
      case a: sbt.testing.NestedTestSelector   => NestedTestSelector(a.suiteId(), a.testName())
      case _: sbt.testing.SuiteSelector        => SuiteSelector()
      case a: sbt.testing.TestSelector         => TestSelector(a.testName())
      case a: sbt.testing.TestWildcardSelector => TestWildcardSelector(a.testWildcard())
    }
}

object ProcessManager extends Logging {
  private val classPathSeparator = java.io.File.pathSeparator

  def newProcess(classpath: Seq[String])(implicit timer: Timer[IO]): ProcessManager = {
    val socketConfig = TestProcessConfig(13337)

    val mainClass = "stryker4s.sbt.testrunner.SbtTestRunnerMain"
    val sysProps = s"-D${TestProcessProperties.port}=${socketConfig.port}"
    val args = Seq(sysProps, mainClass)
    val classpathString = classpath.mkString(classPathSeparator)
    val javaOpts = Seq("-XX:+CMSClassUnloadingEnabled", "-Xms512M", "-Xss8192k", "-XX:MaxPermSize=6G", "-Xmx6G")
    val command = Seq("java", "-cp", classpathString) ++ javaOpts ++ args
    debug(s"Starting process ${command.mkString(" ")}")

    // Wait for 2 seconds before starting
    val messageHandler = (IO.sleep(2.seconds) *>
      retryWithBackoff(6, 3.seconds, info("Could not connect to testprocess. Retrying...")) {
        IO {
          info("Attempting connection to testprocess")
          val startedProcess = scala.sys.process.Process(command)

          val p = startedProcess.run(ProcessLogger(m => debug(s"testrunner: $m")))
          debug("Started process")

          connectToProcess(p, socketConfig)
        }
      }).unsafeRunSync()

    new ProcessManager(messageHandler)
  }

  private def connectToProcess(process: Process, config: TestProcessConfig): TestProcess = {
    debug("Connecting to socket")
    val socket = Socket(InetAddress.getLocalHost(), config.port).either.right.get

    new SocketProcess(process, socket)
  }

  def retryWithBackoff[T](times: Int, backoffPeriod: FiniteDuration, onError: => Unit)(
      f: IO[T]
  )(implicit timer: Timer[IO]): IO[T] = {
    val retriableWithOnError = (NonFatal.apply(_)).compose((t: Throwable) => { onError; t })

    (fs2.Stream.sleep(2.seconds) >>
      fs2.Stream
        // Exponential backoff
        .retry(f, backoffPeriod, d => d * 2, times, retriableWithOnError)).compile.toVector
      .map(_.head)
  }

}

sealed trait TestProcess {
  def sendMessage(request: Request): Response
}

final class SocketProcess(private val process: Process, socket: Socket) extends TestProcess with Logging {

  val objectOutputStream = new ObjectOutputStream(socket.outputStream())
  val objectInputStream = new ObjectInputStream(socket.inputStream())

  override def sendMessage(request: Request): Response = {
    debug(s"Sending message $request")
    objectOutputStream.writeObject(request)
    // Block until a response is read.
    objectInputStream.readObject() match {
      case response: Response => response
      case other =>
        throw new MutationRunFailedException(
          s"Expected an object of type 'Response' from sub-process, but received $other"
        )
    }
  }
}
