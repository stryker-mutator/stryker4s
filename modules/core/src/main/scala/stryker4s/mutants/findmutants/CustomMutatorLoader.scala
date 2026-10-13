package stryker4s.mutants.findmutants

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all.*
import stryker4s.exception.CustomMutatorPluginLoadException
import stryker4s.plugin.CustomMutatorPlugin
import stryker4s.plugin.api.CustomMutator

import java.util.{Iterator as JIterator, ServiceConfigurationError, ServiceLoader}
import scala.annotation.tailrec

/** Loads [[stryker4s.plugin.api.CustomMutator]]s provided through the plugin API service.
  */
object CustomMutatorLoader {
  private type Loaded = Either[CustomMutatorPluginLoadException, List[CustomMutator]]

  /** Loads all plugin-provided mutators using `classLoader` to resolve service providers. Every provider that fails to
    * load is returned in the `Left`.
    */
  def load(classLoader: ClassLoader): IO[Either[NonEmptyList[CustomMutatorPluginLoadException], List[CustomMutator]]] =
    IO.blocking {
      val providers = ServiceLoader.load(classOf[CustomMutatorPlugin], classLoader).iterator()
      val (errors, mutators) = loadAll(providers, Vector.empty).partitionEither(identity)
      NonEmptyList.fromList(errors.toList).toLeft(mutators.flatten.toList)
    }

  @tailrec
  private def loadAll(providers: JIterator[CustomMutatorPlugin], acc: Vector[Loaded]): Vector[Loaded] =
    nextProvider(providers) match {
      case None         => acc
      case Some(result) => loadAll(providers, acc :+ result.flatMap(mutatorsOf))
    }

  // A broken provider is skipped by the iterator on the next call, so iteration can continue after an error
  private def nextProvider(
      providers: JIterator[CustomMutatorPlugin]
  ): Option[Either[CustomMutatorPluginLoadException, CustomMutatorPlugin]] =
    try
      if (providers.hasNext) providers.next().asRight.some
      else none
    catch {
      case e: ServiceConfigurationError => CustomMutatorPluginLoadException(e).asLeft.some
      case e: LinkageError              => CustomMutatorPluginLoadException(e).asLeft.some
    }

  private def mutatorsOf(plugin: CustomMutatorPlugin): Loaded =
    try plugin.mutators.toList.asRight
    catch {
      case e: ServiceConfigurationError => CustomMutatorPluginLoadException(e).asLeft
      case e: LinkageError              => CustomMutatorPluginLoadException(e).asLeft
    }
}
