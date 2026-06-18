package stryker4s.maven.runner

import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint}
import stryker4s.log.Logger
import stryker4s.testrunner.api.*

import java.io.File
import java.lang.annotation.Annotation
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import scala.util.Try
import scala.util.control.NonFatal

/** Discovers `sbt.testing` tests in the compiled test classes and maps them to the protobuf
  * [[stryker4s.testrunner.api.TestGroup]] format sent to the test-runner process.
  *
  * Unlike sbt and Mill, Maven has no notion of `sbt.testing` frameworks, so we replicate what they get from the build
  * tool: probe a list of known framework classes on the test classpath, then scan the compiled test classes for
  * fingerprint matches (the same discovery sbt does from its zinc analysis).
  */
object MavenTestDiscovery {

  /** Known `sbt.testing.Framework` implementations, ported from sbt's default framework list (`sbt.TestFramework`) plus
    * a few common modern frameworks. Each is probed on the test classpath; the ones that load are used.
    */
  val defaultFrameworks: Seq[String] = Seq(
    "org.scalatest.tools.Framework",
    "org.scalatest.tools.ScalaTestFramework",
    "munit.Framework",
    "weaver.framework.CatsEffect",
    "utest.runner.Framework",
    "zio.test.sbt.ZTestFramework",
    "org.scalacheck.ScalaCheckFramework",
    "hedgehog.sbt.Framework",
    "org.specs2.runner.Specs2Framework",
    "org.specs2.runner.SpecsFramework",
    "org.specs.runner.SpecsFramework",
    "com.novocode.junit.JUnitFramework"
  )

  def discover(
      testClassesDirs: Seq[Path],
      runClasspath: Seq[Path],
      frameworkNames: Seq[String] = defaultFrameworks
  )(using log: Logger): IO[Seq[TestGroup]] = {
    val urls = runClasspath.map(_.toNioPath.toUri().toURL()).toArray
    Resource
      .fromAutoCloseable(IO(new URLClassLoader(urls, sbtTestingSharingLoader)))
      .use { classLoader =>
        for {
          frameworks <- frameworkNames.flatTraverse(loadFramework(_, classLoader).map(_.toSeq))
          _ <- IO.whenA(frameworks.isEmpty)(
            IO(
              log.warn(
                "No sbt.testing test frameworks found on the test classpath. " +
                  "Will likely result in no tests being run and a NoCoverage result for all mutants."
              )
            )
          )
          classNames <- testClassesDirs.flatTraverse(scanClassNames)
        } yield frameworks.flatMap { framework =>
          val taskDefs = discoverTaskDefs(framework, classNames, classLoader)
          if taskDefs.isEmpty then None
          else TestGroup(framework.getClass().getName(), taskDefs, RunnerOptions(Seq.empty, Seq.empty).some).some
        }
      }
  }

  private def loadFramework(name: String, classLoader: ClassLoader)(using log: Logger): IO[Option[Framework]] =
    IO.blocking {
      val cls = Class.forName(name, false, classLoader)
      if classOf[Framework].isAssignableFrom(cls) then
        cls.getDeclaredConstructor().newInstance().asInstanceOf[Framework].some
      else none
    }.recoverWith {
      case _: ClassNotFoundException | _: NoClassDefFoundError | _: LinkageError | _: ReflectiveOperationException =>
        IO.pure(none)
      case e if NonFatal(e) =>
        IO.delay(log.debug(s"Could not instantiate test framework $name", e)).as(none)
    }

  private def discoverTaskDefs(
      framework: Framework,
      classNames: Seq[String],
      classLoader: ClassLoader
  ): Seq[TaskDefinition] = {
    val fingerprints = framework.fingerprints().toSeq
    classNames.distinct.flatMap { className =>
      loadClass(className, classLoader).flatMap(cls =>
        matchFingerprint(cls, fingerprints).map { fingerprint =>
          TaskDefinition(
            cls.getName().stripSuffix("$"),
            toApiFingerprint(fingerprint),
            explicitlySpecified = false,
            Seq(SuiteSelector())
          )
        }
      )
    }
  }

  private def loadClass(name: String, classLoader: ClassLoader): Option[Class[?]] =
    Try(Class.forName(name, false, classLoader)).toOption

  /** Find the first fingerprint a class matches, if any. Mirrors sbt's test discovery. */
  private def matchFingerprint(cls: Class[?], fingerprints: Seq[Fingerprint]): Option[Fingerprint] = {
    if Modifier.isAbstract(cls.getModifiers()) || cls.isInterface() then none
    else {
      val module = isModule(cls)
      fingerprints.find {
        case sub: SubclassFingerprint =>
          sub.isModule() == module &&
          (!sub.requireNoArgConstructor() || hasNoArgConstructor(cls)) &&
          superclassMatches(cls, sub.superclassName())
        case ann: AnnotatedFingerprint =>
          ann.isModule() == module && annotationMatches(cls, ann.annotationName())
        case _ => false
      }
    }
  }

  private def isModule(cls: Class[?]): Boolean =
    cls.getName().endsWith("$") && cls.getFields().exists(_.getName() == "MODULE$")

  private def hasNoArgConstructor(cls: Class[?]): Boolean =
    cls.getConstructors().exists(_.getParameterCount() == 0)

  private def superclassMatches(cls: Class[?], superclassName: String): Boolean =
    Option(cls.getClassLoader())
      .flatMap(cl => loadClass(superclassName, cl))
      .exists(parent => parent.isAssignableFrom(cls) && parent != cls)

  private def annotationMatches(cls: Class[?], annotationName: String): Boolean =
    Option(cls.getClassLoader()).flatMap(cl => loadClass(annotationName, cl)).exists {
      case annotation if annotation.isAnnotation() =>
        val annotationClass = annotation.asInstanceOf[Class[? <: Annotation]]
        cls.isAnnotationPresent(annotationClass) ||
        cls.getMethods().exists(_.isAnnotationPresent(annotationClass))
      case _ => false
    }

  /** All fully-qualified class names found in a directory of compiled classes. */
  private def scanClassNames(base: Path): IO[Seq[String]] =
    Files[IO]
      .isDirectory(base)
      .ifM(
        Files[IO]
          .walk(base)
          .filter(_.extName == ".class")
          .map(p => base.relativize(p).toString.stripSuffix(".class").replace(File.separatorChar, '.'))
          // Skip synthetic / anonymous / generated classes (lambdas, `package`/`module` info, …)
          .filterNot(name => name.contains("$$") || name.endsWith("package") || name.endsWith("-info"))
          .compile
          .toList,
        IO.pure(Seq.empty)
      )

  private def platformLoader: ClassLoader =
    // Java 9+: the platform classloader (no application classes). On Java 8 this is the ext loader.
    Option(ClassLoader.getSystemClassLoader()).map(_.getParent()).orNull

  /** Parent loader for the discovery classpath that exposes only the plugin realm's `sbt.testing.*`
    */
  private def sbtTestingSharingLoader: ClassLoader = {
    val pluginLoader = getClass().getClassLoader()
    new ClassLoader(platformLoader) {
      override def findClass(name: String): Class[?] =
        if name.startsWith("sbt.testing.") then pluginLoader.loadClass(name)
        else throw new ClassNotFoundException(name)
    }
  }

  private def toApiFingerprint(fp: Fingerprint): stryker4s.testrunner.api.Fingerprint =
    fp match {
      case a: AnnotatedFingerprint => stryker4s.testrunner.api.AnnotatedFingerprint(a.isModule(), a.annotationName())
      case s: SubclassFingerprint  =>
        stryker4s.testrunner.api.SubclassFingerprint(s.isModule(), s.superclassName(), s.requireNoArgConstructor())
      case other => throw new IllegalArgumentException(s"Unknown fingerprint type: $other")
    }
}
