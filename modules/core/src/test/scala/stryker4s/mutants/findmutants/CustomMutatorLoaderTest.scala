package stryker4s.mutants.findmutants

import stryker4s.exception.CustomMutatorPluginLoadException
import stryker4s.pluginapi.{CustomMutator, CustomMutatorPlugin}
import stryker4s.testkit.Stryker4sSuite

import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class CustomMutatorLoaderTest extends Stryker4sSuite {
  test("load should discover mutators from service providers") {
    val result = CustomMutatorLoader.load(getClass.getClassLoader)

    assertEquals(result.map(_.getClass), List(classOf[CustomMutatorLoaderTestFixture]))
  }

  test("load should wrap a provider that cannot be found") {
    withProviders("does.not.Exist") { classLoader =>
      val e = intercept[CustomMutatorPluginLoadException](CustomMutatorLoader.load(classLoader))
      assert(e.getCause.isInstanceOf[java.util.ServiceConfigurationError], e.getCause)
    }
  }

  test("load should wrap a linkage error raised while providing mutators") {
    withProviders(classOf[LinkageErrorTestPlugin].getName) { classLoader =>
      val e = intercept[CustomMutatorPluginLoadException](CustomMutatorLoader.load(classLoader))
      assert(e.getCause.isInstanceOf[NoClassDefFoundError], e.getCause)
    }
  }

  private def withProviders(providers: String*)(body: ClassLoader => Unit): Unit = {
    val root = Files.createTempDirectory("custom-mutator-loader")
    val services = Files.createDirectories(root.resolve("META-INF/services"))
    Files.write(
      services.resolve(classOf[CustomMutatorPlugin].getName),
      providers.mkString("\n").getBytes(StandardCharsets.UTF_8)
    )
    val classLoader = new URLClassLoader(Array(root.toUri.toURL), getClass.getClassLoader)
    try body(classLoader)
    finally classLoader.close()
  }
}

class CustomMutatorLoaderTestPlugin extends CustomMutatorPlugin {
  override def mutators: List[CustomMutator] = List(new CustomMutatorLoaderTestFixture)
}

class LinkageErrorTestPlugin extends CustomMutatorPlugin {
  override def mutators: List[CustomMutator] = throw new NoClassDefFoundError("scala/meta/Tree")
}

class CustomMutatorLoaderTestFixture extends CustomMutator {
  def matcher: stryker4s.pluginapi.MutationMatcher = PartialFunction.empty
}
