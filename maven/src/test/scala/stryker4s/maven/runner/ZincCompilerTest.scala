package stryker4s.maven.runner

import cats.effect.IO
import stryker4s.testkit.Stryker4sIOSuite

import java.io.File

class ZincCompilerTest extends Stryker4sIOSuite {

  private val jars = Seq(
    new File("scala-library-2.13.18.jar"),
    new File("scala3-library_3-3.3.4.jar"),
    new File("scala-reflect-2.13.18.jar"),
    new File("scala-compiler-2.13.18.jar"),
    new File("compiler-interface-2.0.0.jar"),
    new File("util-interface-2.0.0.jar")
  )

  test("makeScalaInstance excludes the compiler-interface and util-interface jars from allJars") {
    ZincCompiler.makeScalaInstance("2.13.18", jars).use { instance =>
      IO {
        val names = instance.allJars.map(_.getName()).toSet
        assert(!names.contains("compiler-interface-2.0.0.jar"), names)
        assert(!names.contains("util-interface-2.0.0.jar"), names)
        assert(names.contains("scala-compiler-2.13.18.jar"), names)
        assert(names.contains("scala-library-2.13.18.jar"), names)
      }
    }
  }

  test("makeScalaInstance picks only the scala-library/scala3-library/scala-reflect jars as libraryJars") {
    ZincCompiler.makeScalaInstance("2.13.18", jars).use { instance =>
      IO {
        assertEquals(
          instance.libraryJars.map(_.getName()).toSet,
          Set("scala-library-2.13.18.jar", "scala3-library_3-3.3.4.jar", "scala-reflect-2.13.18.jar")
        )
      }
    }
  }

  test("makeScalaInstance does not treat the scala-compiler jar as a library jar") {
    ZincCompiler.makeScalaInstance("2.13.18", jars).use { instance =>
      IO {
        assert(!instance.libraryJars.map(_.getName()).contains("scala-compiler-2.13.18.jar"))
      }
    }
  }

  test("makeScalaInstance carries the given Scala version") {
    ZincCompiler.makeScalaInstance("2.13.18", jars).use { instance =>
      IO(assertEquals(instance.version, "2.13.18"))
    }
  }
}
