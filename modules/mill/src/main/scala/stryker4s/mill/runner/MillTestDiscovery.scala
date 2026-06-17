package stryker4s.mill.runner

import cats.syntax.option.*
import mill.javalib.testrunner.{Framework, TestRunnerUtils}
import mill.util.Jvm
import stryker4s.log.Logger
import stryker4s.testrunner.api.*

import scala.util.Using

/** Discovers tests in a compiled test module and maps them to the protobuf [[TestGroup]] format that is sent to the
  * test-runner process.
  *
  * Mill has a single test framework per test module (unlike sbt), so this always results in a single [[TestGroup]].
  */
object MillTestDiscovery {

  /** @param frameworkName
    *   the class name of the test framework (`TestModule#testFramework`)
    * @param testClasspath
    *   classpath entries containing the compiled test classes to scan for tests
    * @param runClasspath
    *   the full classpath the test classes (and framework) can be loaded with
    */
  def discover(
      frameworkName: String,
      testClasspath: Seq[os.Path],
      runClasspath: Seq[os.Path]
  )(implicit log: Logger): Seq[TestGroup] = {
    // Parent-first classloader so `sbt.testing` interfaces are shared with this plugin
    Using.resource(Jvm.createClassLoader(runClasspath, parent = getClass().getClassLoader())) { classLoader =>
      val framework = Framework.framework(frameworkName)(classLoader)
      // Note: `TestRunnerUtils` is marked `@internal` in Mill, but is the same discovery Mill itself uses to run tests
      val discovered = TestRunnerUtils.discoverTests(classLoader, framework, testClasspath, none)

      if discovered.isEmpty then
        log.warn(
          s"No tests found for test framework $frameworkName. " +
            "Will likely result in no tests being run and a NoCoverage result for all mutants."
        )

      val taskDefs = discovered.map { case (cls, fingerprint) =>
        TaskDefinition(
          cls.getName().stripSuffix("$"),
          toApiFingerprint(fingerprint),
          explicitlySpecified = false,
          Seq(SuiteSelector())
        )
      }
      val runnerOptions = RunnerOptions(Seq.empty, Seq.empty)
      Seq(TestGroup(framework.getClass().getCanonicalName(), taskDefs, runnerOptions.some))
    }
  }

  private def toApiFingerprint(fp: sbt.testing.Fingerprint): Fingerprint =
    fp match {
      case a: sbt.testing.AnnotatedFingerprint => AnnotatedFingerprint(a.isModule(), a.annotationName())
      case s: sbt.testing.SubclassFingerprint  =>
        SubclassFingerprint(s.isModule(), s.superclassName(), s.requireNoArgConstructor())
      case other: sbt.testing.Fingerprint => throw new IllegalArgumentException(s"Unknown fingerprint type: $other")
    }
}
