package stryker4s.maven.runner

import cats.effect.IO
import fs2.io.file.{Files, Path}
import stryker4s.testkit.{LogMatchers, Stryker4sIOSuite}

import java.io.File

class MavenTestDiscoveryTest extends Stryker4sIOSuite with LogMatchers {

  /** The directory holding this test module's own compiled classes. */
  private def testClassesDir: Path =
    Path.fromNioPath(java.nio.file.Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()))

  private def fullClasspath: Seq[Path] =
    sys.props("java.class.path").split(File.pathSeparator).filter(_.nonEmpty).map(Path(_)).toSeq

  test("discovers munit test suites and maps them to a TestGroup") {
    MavenTestDiscovery.discover(Seq(testClassesDir), fullClasspath).map { groups =>
      val munitGroup = groups
        .find(_.frameworkClass == "munit.Framework")
        .getOrElse(fail(s"Expected a munit.Framework group, got: ${groups.map(_.frameworkClass)}"))
      assert(
        munitGroup.taskDefs.exists(_.fullyQualifiedName == "stryker4s.maven.runner.MavenTestDiscoveryTest"),
        s"This suite should be discovered, but got: ${munitGroup.taskDefs.map(_.fullyQualifiedName)}"
      )
    }
  }

  test("warns when no test frameworks are found on the classpath") {
    MavenTestDiscovery
      .discover(Seq(testClassesDir), fullClasspath, frameworkNames = Seq("does.not.Exist"))
      .map { groups =>
        assertEquals(groups, Seq.empty)
        assertLoggedWarn("No sbt.testing test frameworks found on the test classpath")
        assertLoggedWarn("Will likely result in no tests being run and a NoCoverage result for all mutants.")
      }
  }

  test("a framework name that resolves to a non-Framework class is skipped without failing") {
    MavenTestDiscovery
      .discover(Seq(testClassesDir), fullClasspath, frameworkNames = Seq(fixtures + "UnrelatedClass"))
      .map(groups => assertEquals(groups, Seq.empty))
  }

  private val fixtures = "stryker4s.maven.runner.fixtures."

  private def discoverWith(framework: String) =
    MavenTestDiscovery.discover(Seq(testClassesDir), fullClasspath, frameworkNames = Seq(framework))

  private def discoveredNames(framework: String): IO[Set[String]] =
    discoverWith(framework).map(_.flatMap(_.taskDefs.map(_.fullyQualifiedName)).toSet)

  test("a subclass fingerprint discovers concrete, no-arg subclasses of the fingerprint superclass") {
    discoveredNames(fixtures + "FakeSubclassFramework").map { names =>
      assert(names.contains(fixtures + "ConcreteFakeTest"), names)
    }
  }

  test("a subclass fingerprint does not discover abstract classes") {
    discoveredNames(fixtures + "FakeSubclassFramework").map { names =>
      assert(!names.contains(fixtures + "AbstractFakeTest"), names)
    }
  }

  test("a subclass fingerprint does not discover unrelated classes") {
    discoveredNames(fixtures + "FakeSubclassFramework").map { names =>
      assert(!names.contains(fixtures + "UnrelatedClass"), names)
    }
  }

  test("a non-module subclass fingerprint does not discover module (object) classes") {
    discoveredNames(fixtures + "FakeSubclassFramework").map { names =>
      assert(!names.contains(fixtures + "FakeModuleTest"), names)
    }
  }

  test("a no-arg-constructor-requiring fingerprint does not discover classes without a no-arg constructor") {
    discoveredNames(fixtures + "FakeSubclassFramework").map { names =>
      assert(!names.contains(fixtures + "NoArgLessFakeTest"), names)
    }
  }

  test("a no-arg-requiring fingerprint discovers a class that has a no-arg constructor among others") {
    discoveredNames(fixtures + "FakeSubclassFramework").map { names =>
      assert(names.contains(fixtures + "MultiCtorFakeTest"), names)
    }
  }

  test("a fingerprint that does not require a no-arg constructor discovers classes without one") {
    discoveredNames(fixtures + "FakeNoCtorRequirementFramework").map { names =>
      assert(names.contains(fixtures + "NoArgLessFakeTest"), names)
      assert(names.contains(fixtures + "ConcreteFakeTest"), names)
    }
  }

  test("a module fingerprint discovers module (object) classes and strips the trailing $ from the name") {
    discoveredNames(fixtures + "FakeModuleFramework").map { names =>
      assert(names.contains(fixtures + "FakeModuleTest"), names)
      assert(!names.contains(fixtures + "FakeModuleTest$"), names)
    }
  }

  test("a module fingerprint does not discover non-module classes") {
    discoveredNames(fixtures + "FakeModuleFramework").map { names =>
      assert(!names.contains(fixtures + "ConcreteFakeTest"), names)
    }
  }

  test("a fingerprint with an unloadable superclass matches nothing") {
    discoveredNames(fixtures + "FakeUnloadableSuperclassFramework").map { names =>
      assertEquals(names, Set.empty[String])
    }
  }

  test("discovered task definitions are not explicitly specified") {
    discoverWith(fixtures + "FakeSubclassFramework").map { groups =>
      val taskDefs = groups.flatMap(_.taskDefs)
      assert(taskDefs.nonEmpty)
      assert(taskDefs.forall(!_.explicitlySpecified), taskDefs.map(_.explicitlySpecified))
    }
  }

  test("a framework whose fingerprints match nothing yields no test group") {
    Files[IO].tempDirectory.use { emptyDir =>
      MavenTestDiscovery
        .discover(Seq(emptyDir), fullClasspath, frameworkNames = Seq(fixtures + "FakeSubclassFramework"))
        .map(groups => assertEquals(groups, Seq.empty))
    }
  }
}
