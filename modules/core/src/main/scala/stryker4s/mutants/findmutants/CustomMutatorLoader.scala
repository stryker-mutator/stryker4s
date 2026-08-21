package stryker4s.mutants.findmutants

import stryker4s.exception.{
  CustomMutatorClassNotFoundException,
  CustomMutatorInstantiationException,
  CustomMutatorNotAssignableException
}
import stryker4s.mutatorapi.CustomMutator

/** Reflectively instantiates [[stryker4s.mutatorapi.CustomMutator]]s configured via `Config.customMutators`.
  */
object CustomMutatorLoader {

  /** Loads the given fully-qualified class names as [[stryker4s.mutatorapi.CustomMutator]] instances, using
    * `classLoader` to resolve them.
    *
    * @throws stryker4s.exception.CustomMutatorClassNotFoundException
    *   if a class name can not be found on `classLoader`
    * @throws stryker4s.exception.CustomMutatorNotAssignableException
    *   if a class does not extend [[stryker4s.mutatorapi.CustomMutator]]
    * @throws stryker4s.exception.CustomMutatorInstantiationException
    *   if a class can not be instantiated (e.g. no public no-argument constructor)
    */
  def load(classNames: Seq[String], classLoader: ClassLoader): List[CustomMutator] =
    classNames.map(loadOne(_, classLoader)).toList

  private def loadOne(className: String, classLoader: ClassLoader): CustomMutator = {
    val clazz =
      try classLoader.loadClass(className)
      catch { case _: ClassNotFoundException => throw CustomMutatorClassNotFoundException(className) }

    if (!classOf[CustomMutator].isAssignableFrom(clazz))
      throw CustomMutatorNotAssignableException(className)

    try clazz.getDeclaredConstructor().newInstance().asInstanceOf[CustomMutator]
    catch {
      case e: ReflectiveOperationException => throw CustomMutatorInstantiationException(className, e)
    }
  }
}
