package stryker4s.mutants.findmutants

import cats.effect.{IO, Resource}
import stryker4s.plugin.CustomMutatorPlugin
import stryker4s.plugin.api.CustomMutator
import stryker4s.testkit.Stryker4sIOSuite

import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class CustomMutatorLoaderTest extends Stryker4sIOSuite {
  test("load should discover mutators from service providers") {
    CustomMutatorLoader.load(getClass.getClassLoader).map { result =>
      assertEquals(result.value.map(_.getClass), List(classOf[CustomMutatorLoaderTestFixture]))
    }
  }

  test("load should return a provider that cannot be found as a Left") {
    withProviders("does.not.Exist") { classLoader =>
      CustomMutatorLoader.load(classLoader).map { result =>
        assert(result.leftValue.head.getCause.isInstanceOf[java.util.ServiceConfigurationError])
      }
    }
  }

  test("load should return a linkage error raised while providing mutators as a Left") {
    withProviders(classOf[LinkageErrorTestPlugin].getName) { classLoader =>
      CustomMutatorLoader.load(classLoader).map { result =>
        assert(result.leftValue.head.getCause.isInstanceOf[NoClassDefFoundError])
      }
    }
  }

  test("load should collect the errors of all failing providers") {
    withProviders(
      "does.not.Exist",
      classOf[LinkageErrorTestPlugin].getName,
      classOf[CustomMutatorLoaderTestPlugin].getName
    ) { classLoader =>
      CustomMutatorLoader.load(classLoader).map { result =>
        assertEquals(result.leftValue.size, 2)
      }
    }
  }

  private def withProviders(providers: String*)(body: ClassLoader => IO[Unit]): IO[Unit] =
    Resource
      .fromAutoCloseable(IO.blocking {
        val root = Files.createTempDirectory("custom-mutator-loader")
        val services = Files.createDirectories(root.resolve("META-INF/services"))
        Files.write(
          services.resolve(classOf[CustomMutatorPlugin].getName),
          providers.mkString("\n").getBytes(StandardCharsets.UTF_8)
        )
        new URLClassLoader(Array(root.toUri.toURL), getClass.getClassLoader)
      })
      .use(body(_))
}

class CustomMutatorLoaderTestPlugin extends CustomMutatorPlugin {
  override def mutators: List[CustomMutator] = List(new CustomMutatorLoaderTestFixture)
}

class LinkageErrorTestPlugin extends CustomMutatorPlugin {
  override def mutators: List[CustomMutator] = throw new NoClassDefFoundError("scala/meta/Tree")
}

class CustomMutatorLoaderTestFixture extends CustomMutator {
  def matcher: stryker4s.plugin.api.MutationMatcher = PartialFunction.empty
}
