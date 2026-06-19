package stryker4s.mill

import mill.api.Discover
import mill.scalalib.*
import mill.testkit.{TestRootModule, UnitTester}
import mill.util.TokenReaders.*
import stryker4s.testkit.Stryker4sSuite

import java.nio.file.Paths

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
}
