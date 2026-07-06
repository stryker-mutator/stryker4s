package stryker4s.maven.runner

import cats.effect.IO
import fs2.io.file.Path
import stryker4s.testkit.Stryker4sIOSuite

class ZincCompilerTest extends Stryker4sIOSuite {

  private val jars = Seq(
    Path("scala-library-2.13.18.jar"),
    Path("scala3-library_3-3.3.4.jar"),
    Path("scala-reflect-2.13.18.jar"),
    Path("scala-compiler-2.13.18.jar"),
    Path("compiler-interface-2.0.0.jar"),
    Path("util-interface-2.0.0.jar")
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
