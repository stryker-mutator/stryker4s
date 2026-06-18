package stryker4s.maven.runner.fixtures

import sbt.testing.{Fingerprint, Framework, Runner, SubclassFingerprint}

/** Marker base type referenced by the fake frameworks' [[SubclassFingerprint]]s. */
trait FakeTestBase

/** Concrete subclass with a no-arg constructor — the typical discoverable test. */
class ConcreteFakeTest extends FakeTestBase

/** Abstract subclass — must never be discovered (abstract classes are skipped). */
abstract class AbstractFakeTest extends FakeTestBase

/** Subclass without a no-arg constructor. */
class NoArgLessFakeTest(val x: Int) extends FakeTestBase

/** Subclass with both a no-arg and an argument-taking constructor. */
class MultiCtorFakeTest(val x: Int) extends FakeTestBase {
  def this() = this(0)
}

/** Module (object) subclass — discovered only by module fingerprints. */
object FakeModuleTest extends FakeTestBase

/** Unrelated class that matches no fingerprint. */
class UnrelatedClass

/** A [[SubclassFingerprint]] for [[FakeTestBase]] with configurable module/no-arg-ctor flags. */
final class FakeSubclassFingerprint(module: Boolean, requireNoArg: Boolean, superclass: String)
    extends SubclassFingerprint {
  override def isModule(): Boolean = module
  override def superclassName(): String = superclass
  override def requireNoArgConstructor(): Boolean = requireNoArg
}

/** Base for fake frameworks; only `fingerprints` and the class name are used by discovery. */
abstract class FakeFrameworkBase extends Framework {
  override def name(): String = getClass().getName()
  override def runner(args: Array[String], remoteArgs: Array[String], testClassLoader: ClassLoader): Runner =
    throw new UnsupportedOperationException("not used in discovery tests")
}

/** Matches non-module subclasses of FakeTestBase that have a no-arg constructor. */
class FakeSubclassFramework extends FakeFrameworkBase {
  override def fingerprints(): Array[Fingerprint] =
    Array(
      new FakeSubclassFingerprint(module = false, requireNoArg = true, "stryker4s.maven.runner.fixtures.FakeTestBase")
    )
}

/** Matches module subclasses of FakeTestBase. */
class FakeModuleFramework extends FakeFrameworkBase {
  override def fingerprints(): Array[Fingerprint] =
    Array(
      new FakeSubclassFingerprint(module = true, requireNoArg = false, "stryker4s.maven.runner.fixtures.FakeTestBase")
    )
}

/** Matches non-module subclasses of FakeTestBase regardless of constructor arity. */
class FakeNoCtorRequirementFramework extends FakeFrameworkBase {
  override def fingerprints(): Array[Fingerprint] =
    Array(
      new FakeSubclassFingerprint(module = false, requireNoArg = false, "stryker4s.maven.runner.fixtures.FakeTestBase")
    )
}

/** Has a fingerprint whose superclass cannot be loaded — should match nothing. */
class FakeUnloadableSuperclassFramework extends FakeFrameworkBase {
  override def fingerprints(): Array[Fingerprint] =
    Array(new FakeSubclassFingerprint(module = false, requireNoArg = true, "does.not.Exist"))
}
