package stryker4s.mutants.findmutants

import stryker4s.exception.CustomMutatorPluginLoadException
import stryker4s.pluginapi.{CustomMutator, CustomMutatorPlugin}

import java.util.ServiceLoader
import scala.jdk.CollectionConverters.*

/** Loads [[stryker4s.pluginapi.CustomMutator]]s provided through the plugin API service.
  */
object CustomMutatorLoader {

  /** Loads all plugin-provided mutators using `classLoader` to resolve service providers.
    */
  def load(classLoader: ClassLoader): List[CustomMutator] =
    try ServiceLoader.load(classOf[CustomMutatorPlugin], classLoader).iterator().asScala.toList.flatMap(_.mutators)
    catch {
      case e: java.util.ServiceConfigurationError => throw CustomMutatorPluginLoadException(e)
      case e: LinkageError                        => throw CustomMutatorPluginLoadException(e)
    }
}
