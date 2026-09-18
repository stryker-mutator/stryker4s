package stryker4s.mutants.findmutants

import stryker4s.exception.{
  CustomMutatorClassNotFoundException,
  CustomMutatorIncompatibleException,
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
    * @throws stryker4s.exception.CustomMutatorIncompatibleException
    *   if a class (or one of its dependencies) fails to link, which usually indicates it was compiled against an
    *   incompatible Scala or `stryker4s-mutator-api` binary version than the one Stryker4s is running with
    */
  def load(classNames: Seq[String], classLoader: ClassLoader): List[CustomMutator] =
    classNames.map(loadOne(_, classLoader)).toList

  private def loadOne(className: String, classLoader: ClassLoader): CustomMutator = {
    val clazz =
      try classLoader.loadClass(className)
      catch {
        case _: ClassNotFoundException => throw CustomMutatorClassNotFoundException(className)
        case e: LinkageError           => throw CustomMutatorIncompatibleException(className, e)
      }

    if (!classOf[CustomMutator].isAssignableFrom(clazz))
      throw CustomMutatorNotAssignableException(className)

    try clazz.getDeclaredConstructor().newInstance().asInstanceOf[CustomMutator]
    catch {
      // The JVM wraps any Throwable thrown by the constructor itself (including a LinkageError) in an
      // InvocationTargetException; unwrap that case first so it is reported as an incompatibility rather than a
      // generic instantiation failure. A LinkageError can also propagate directly (unwrapped), e.g. as an
      // ExceptionInInitializerError from a failing static/companion-object initializer.
      case e: java.lang.reflect.InvocationTargetException if e.getCause.isInstanceOf[LinkageError] =>
        throw CustomMutatorIncompatibleException(className, e.getCause)
      case e: ReflectiveOperationException => throw CustomMutatorInstantiationException(className, e)
      case e: LinkageError                 => throw CustomMutatorIncompatibleException(className, e)
    }
  }
}
