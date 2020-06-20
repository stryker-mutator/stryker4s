package stryker4s.sbt.runner

import scala.tools.nsc.io.Socket
import stryker4s.model.MutantRunResult
import java.nio.file.Path
import grizzled.slf4j.Logging
import scala.sys.process.Process
import stryker4s.api.testprocess._
import stryker4s.model._
import scala.sys.process.ProcessLogger
import java.io.Closeable
import java.io.ObjectOutputStream
import java.io.ObjectInputStream
import sbt.{Tests, TestDefinition => SbtTestDefinition}
import stryker4s.extension.exception.MutationRunFailedException
import sbt.{TestFramework => SbtTestFramework}
import sbt.testing.{Framework => SbtFramework}
import java.net.InetAddress

class ProcessManager(testProcess: TestProcess) extends Closeable with Logging {

  def runMutant(mutant: Mutant, path: Path): MutantRunResult = {
    val message = StartTestRun(Some(mutant.id))
    testProcess.sendMessage(message) match {
      case _: TestsSuccessful   => Survived(mutant, path)
      case _: TestsUnsuccessful => Killed(mutant, path)
      case _                    => Error(mutant, path)
    }
  }

  def initialTestRun(frameworks: Seq[SbtFramework], testGroups: Seq[Tests.Group]): Boolean = {
    val apiTestGroups = TestProcessContext(toApiTestGroups(frameworks, testGroups))

    testProcess.sendMessage(SetupTestContext(apiTestGroups))

    testProcess.sendMessage(StartTestRun(None)) match {
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

  def close(): Unit = {
    testProcess.close()
  }
}

object ProcessManager extends Logging {
  private val classPathSeparator = java.io.File.pathSeparator

  def newProcess(classpath: Seq[String]): ProcessManager = {
    val socketConfig = TestProcessConfig(13337)

    val mainClass = "stryker4s.sbt.testrunner.SbtTestRunnerMain"
    val args = mainClass +: socketConfig.toArgs
    val commandRunnerAndApi = Seq(
      // TODO: Resolve dependency locations in some other way
      "/home/hugovr/.ivy2/local/io.stryker-mutator/stryker4s-api_2.12/0.8.1-SNAPSHOT/jars/stryker4s-api_2.12.jar",
      "/home/hugovr/.ivy2/local/io.stryker-mutator/sbt-stryker4s-testrunner_2.12/0.8.1-SNAPSHOT/jars/sbt-stryker4s-testrunner_2.12.jar",
      "/home/hugovr/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/test-interface/1.0/test-interface-1.0.jar"
    )
    val completeClasspath = (classpath ++ commandRunnerAndApi).distinct.mkString(classPathSeparator)
    val javaOpts = Seq("-XX:+CMSClassUnloadingEnabled", "-Xms512M", "-Xss8192k", "-XX:MaxPermSize=6G", "-Xmx6G")
    val command = Seq("java", "-cp", completeClasspath) ++ javaOpts ++ args
    debug(s"Starting process ${command.mkString(" ")}")

    val startedProcess = scala.sys.process.Process(command)

    val p = startedProcess.run(ProcessLogger(m => info(s"testrunner: $m")))
    info("Started process")
    Thread.sleep(10000) // TODO: Ugly! Wait until process is ready to accept connection in some other way
    val messageHandler = connectToProcess(p, socketConfig)

    new ProcessManager(messageHandler)
  }
  private def connectToProcess(process: Process, config: TestProcessConfig): TestProcess = {
    info("Connecting to socket")
    val socket = Socket(InetAddress.getLocalHost(), config.port).opt.get

    new SocketProcess(process, socket)
  }
}

sealed trait TestProcess extends Closeable {
  def sendMessage(request: Request): Response
}

final class SocketProcess(private val process: Process, socket: Socket) extends TestProcess with Logging {

  val objectOutputStream = new ObjectOutputStream(socket.outputStream())
  val objectInputStream = new ObjectInputStream(socket.inputStream())

  override def sendMessage(request: Request): Response = {
    info(s"Sending message $request")
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

  override def close(): Unit = {
    objectOutputStream.close()
    objectInputStream.close()
    socket.close()
    process.destroy()
  }
}
