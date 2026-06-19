package stryker4s.mill

import mill.api.Discover
import mill.scalalib.*
import mill.testkit.{TestRootModule, UnitTester}
import mill.util.TokenReaders.*
import stryker4s.testkit.Stryker4sSuite

import java.nio.file.Paths
import scala.meta.dialects

class Stryker4sModuleTest extends Stryker4sSuite {

  /** Locate a test-resource project directory copied onto the test classpath by sbt. */
  private def resourceProject(name: String): os.Path =
    os.Path(Paths.get(getClass().getClassLoader().getResource(name).toURI()))

  object unitTestProject extends TestRootModule {
    object foo extends ScalaModule, Stryker4sModule {
      def scalaVersion = "3.3.8"

      object test extends ScalaTests {
        def testFramework = "munit.Framework"
      }
    }

    lazy val millDiscover = Discover[this.type]
  }

  object noTestModuleProject extends TestRootModule {
    object bar extends ScalaModule, Stryker4sModule {
      def scalaVersion = "3.3.8"
    }

    lazy val millDiscover = Discover[this.type]
  }

  object multipleTestModuleProject extends TestRootModule {
    object foo extends ScalaModule, Stryker4sModule {
      def scalaVersion = "3.3.8"

      object test extends ScalaTests {
        def testFramework = "munit.Framework"
      }

      object itTest extends ScalaTests {
        def testFramework = "munit.Framework"
      }
    }

    lazy val millDiscover = Discover[this.type]
  }

  object scala2Project extends TestRootModule {
    // Scala 2.13 module with `-Xsource:3` enabled, plus an unrelated option to distinguish exists/forall
    object source3 extends ScalaModule, Stryker4sModule {
      def scalaVersion = "2.13.18"
      override def scalacOptions = Seq("-Xsource:3", "-deprecation")

      object test extends ScalaTests {
        def testFramework = "munit.Framework"
      }
    }
    // Scala 2.13 module without `-Xsource:3`
    object plain extends ScalaModule, Stryker4sModule {
      def scalaVersion = "2.13.18"
      override def scalacOptions = Seq("-deprecation")

      object test extends ScalaTests {
        def testFramework = "munit.Framework"
      }
    }

    lazy val millDiscover = Discover[this.type]
  }

  test("strykerTestModule resolves the child test module") {
    UnitTester(unitTestProject, resourceProject("unit-test-project")).scoped { _ =>
      assertEquals(unitTestProject.foo.strykerTestModule, unitTestProject.foo.test)
    }
  }

  test("strykerTestModule fails with a helpful message when there is no test module") {
    UnitTester(noTestModuleProject, resourceProject("no-test-module-project")).scoped { _ =>
      interceptMessage[RuntimeException](
        "No test module found for module 'bar'. Override with `def strykerTestModule = myTestModule` to point Stryker4s to the test module to run tests with."
      )(noTestModuleProject.bar.strykerTestModule)
    }
  }

  test("strykerTestModule fails with a helpful message when there are multiple test modules") {
    UnitTester(multipleTestModuleProject, resourceProject("unit-test-project")).scoped { _ =>
      interceptMessage[RuntimeException](
        "Multiple test modules found for module 'foo' (itTest, test). Override with `def strykerTestModule = myTestModule` to point Stryker4s to the test module to run tests with."
      )(multipleTestModuleProject.foo.strykerTestModule)
    }
  }

  test("the module discovers its source files to mutate") {
    UnitTester(unitTestProject, resourceProject("unit-test-project")).scoped { eval =>
      val result = eval(unitTestProject.foo.allSourceFiles).value
      assert(
        result.value.exists(_.path.last == "Calc.scala"),
        s"Calc.scala not found in ${result.value.map(_.path)}"
      )
    }
  }

  test("strykerMutate and strykerFiles produce globs relative to the module dir") {
    UnitTester(unitTestProject, resourceProject("unit-test-project")).scoped { eval =>
      assertEquals(eval(unitTestProject.foo.strykerMutate).value.value, Some(Seq("src/**.scala")))
      assertEquals(eval(unitTestProject.foo.strykerFiles).value.value, Some(Seq("src/**")))
    }
  }

  test("strykerScalaDialect derives the scalameta dialect from the scala version") {
    UnitTester(unitTestProject, resourceProject("unit-test-project")).scoped { eval =>
      // scalaVersion is 3.3.8, so the dialect should be Scala 3.3
      assertEquals(eval(unitTestProject.foo.strykerScalaDialect).value.value, Some(dialects.Scala33))
    }
  }

  test("strykerScalaDialect picks the source3 dialect for Scala 2 with -Xsource:3") {
    UnitTester(scala2Project, resourceProject("unit-test-project")).scoped { eval =>
      assertEquals(eval(scala2Project.source3.strykerScalaDialect).value.value, Some(dialects.Scala213Source3))
    }
  }

  test("strykerScalaDialect picks the plain dialect for Scala 2 without -Xsource:3") {
    UnitTester(scala2Project, resourceProject("unit-test-project")).scoped { eval =>
      assertEquals(eval(scala2Project.plain.strykerScalaDialect).value.value, Some(dialects.Scala213))
    }
  }
}
