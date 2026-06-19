package stryker4s.model

import sbt.{TestDefinition, Tests}
import stryker4s.testkit.Stryker4sSuite
import stryker4s.testrunner.api.*

/** A test framework that advertises a fixed set of fingerprints. */
final private class FakeFramework(fps: Array[sbt.testing.Fingerprint]) extends sbt.testing.Framework {
  override def name(): String = "fake"
  override def fingerprints(): Array[sbt.testing.Fingerprint] = fps
  override def runner(
      args: Array[String],
      remoteArgs: Array[String],
      loader: ClassLoader
  ): sbt.testing.Runner = throw new UnsupportedOperationException("runner is not used by the mapper")
}

final private class FakeSubclassFingerprint(module: Boolean, superclass: String, noArgConstructor: Boolean)
    extends sbt.testing.SubclassFingerprint {
  override def isModule(): Boolean = module
  override def superclassName(): String = superclass
  override def requireNoArgConstructor(): Boolean = noArgConstructor
}

final private class FakeAnnotatedFingerprint(module: Boolean, annotation: String)
    extends sbt.testing.AnnotatedFingerprint {
  override def isModule(): Boolean = module
  override def annotationName(): String = annotation
}

/** A selector type the mapper does not recognize, to exercise the failure branch. */
final private class UnknownSelector extends sbt.testing.Selector

class TestInterfaceMapperTest extends Stryker4sSuite {

  private def group(tests: Seq[TestDefinition]): Tests.Group =
    new Tests.Group(name = "group", tests = tests, runPolicy = Tests.InProcess)

  test("maps matching tests into a TestGroup keyed by the framework's class name") {
    val fingerprint = new FakeSubclassFingerprint(module = false, superclass = "munit.Suite", noArgConstructor = true)
    // A second, non-matching fingerprint so that `exists` (not `forall`) is required to assign the test
    val otherFingerprint =
      new FakeSubclassFingerprint(module = true, superclass = "other.Base", noArgConstructor = false)
    val framework = new FakeFramework(Array(fingerprint, otherFingerprint))

    val testDef = new TestDefinition(
      "com.example.MyTest",
      fingerprint,
      false,
      Array[sbt.testing.Selector](new sbt.testing.SuiteSelector, new sbt.testing.TestSelector("a test"))
    )

    val result = TestInterfaceMapper.toApiTestGroups(Seq(framework), Seq(group(Seq(testDef))))

    val testGroup = result.loneElement
    assertEquals(testGroup.frameworkClass, classOf[FakeFramework].getCanonicalName())
    assertEquals(testGroup.runnerOptions, Some(RunnerOptions(Seq.empty, Seq.empty)))
    assertEquals(
      testGroup.taskDefs,
      Seq(
        TaskDefinition(
          "com.example.MyTest",
          SubclassFingerprint(false, "munit.Suite", true),
          false,
          Seq(SuiteSelector(), TestSelector("a test"))
        )
      )
    )
  }

  test("maps every fingerprint and selector type") {
    val annotated = new FakeAnnotatedFingerprint(module = true, annotation = "org.junit.Test")
    val framework = new FakeFramework(Array(annotated))

    val testDef = new TestDefinition(
      "com.example.AnnotatedTest",
      annotated,
      true,
      Array[sbt.testing.Selector](
        new sbt.testing.SuiteSelector,
        new sbt.testing.TestSelector("t"),
        new sbt.testing.NestedSuiteSelector("suite-id"),
        new sbt.testing.NestedTestSelector("suite-id", "nested"),
        new sbt.testing.TestWildcardSelector("wild*")
      )
    )

    val result = TestInterfaceMapper.toApiTestGroups(Seq(framework), Seq(group(Seq(testDef))))

    assertEquals(
      result.loneElement.taskDefs,
      Seq(
        TaskDefinition(
          "com.example.AnnotatedTest",
          AnnotatedFingerprint(true, "org.junit.Test"),
          true,
          Seq(
            SuiteSelector(),
            TestSelector("t"),
            NestedSuiteSelector("suite-id"),
            NestedTestSelector("suite-id", "nested"),
            TestWildcardSelector("wild*")
          )
        )
      )
    )
  }

  test("returns no groups when no framework recognizes the tests") {
    val testFingerprint =
      new FakeSubclassFingerprint(module = false, superclass = "munit.Suite", noArgConstructor = true)
    val frameworkFingerprint =
      new FakeSubclassFingerprint(module = false, superclass = "other.Suite", noArgConstructor = true)
    val framework = new FakeFramework(Array(frameworkFingerprint))
    val testDef = new TestDefinition("x", testFingerprint, false, Array.empty[sbt.testing.Selector])

    val result = TestInterfaceMapper.toApiTestGroups(Seq(framework), Seq(group(Seq(testDef))))

    assertEquals(result, Seq.empty[TestGroup])
  }

  test("throws on an unrecognized selector type") {
    val fingerprint = new FakeSubclassFingerprint(module = false, superclass = "munit.Suite", noArgConstructor = true)
    val framework = new FakeFramework(Array(fingerprint))
    val testDef = new TestDefinition("x", fingerprint, false, Array[sbt.testing.Selector](new UnknownSelector))

    val ex = intercept[IllegalArgumentException](
      TestInterfaceMapper.toApiTestGroups(Seq(framework), Seq(group(Seq(testDef))))
    )
    assert(ex.getMessage().startsWith("Unknown selector type:"), ex.getMessage())
  }
}
