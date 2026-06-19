package stryker4s.mill.runner

import stryker4s.testkit.{LogMatchers, Stryker4sSuite}

import java.io.File
import java.nio.file.Paths

class MillTestDiscoveryTest extends Stryker4sSuite with LogMatchers {

  /** The directory holding this test module's own compiled classes. */
  private def testClassesDir: os.Path =
    os.Path(Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()))

  private def fullClasspath: Seq[os.Path] =
    sys.props("java.class.path").split(File.pathSeparator).filter(_.nonEmpty).map(os.Path(_)).toSeq

  test("discovers munit test suites and maps them to a single TestGroup") {
    val groups = MillTestDiscovery.discover("munit.Framework", Seq(testClassesDir), fullClasspath)

    val group = groups.loneElement
    assertEquals(group.frameworkClass, "munit.Framework")
    val taskDef = group.taskDefs
      .find(_.fullyQualifiedName == "stryker4s.mill.runner.MillTestDiscoveryTest")
      .getOrElse(
        fail(s"This test suite should be discovered, but got: ${group.taskDefs.map(_.fullyQualifiedName)}")
      )
    // Discovered tests are never explicitly specified on the command line
    assertEquals(taskDef.explicitlySpecified, false)
    assertNotLoggedWarn("No tests found for test framework")
  }

  test("warns when no tests are found for the framework") {
    val groups = MillTestDiscovery.discover("munit.Framework", Seq.empty, fullClasspath)

    assertEquals(groups.loneElement.taskDefs, Seq.empty)
    assertLoggedWarn(
      "No tests found for test framework munit.Framework. " +
        "Will likely result in no tests being run and a NoCoverage result for all mutants."
    )
  }
}
