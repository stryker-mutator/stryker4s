package stryker4s.maven.runner

import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.io.file.Path
import org.apache.maven.project.MavenProject
import sbt.internal.inc.{PlainVirtualFileConverter, ScalaInstance, ZincUtil}
import stryker4s.log.{Logger, XsbtiLogger}
import xsbti.*
import xsbti.compile.*
import xsbti.compile.analysis.ReadStamps

import java.io.File
import java.net.URLClassLoader
import java.util.Optional
import scala.collection.mutable.ListBuffer

/** Compiles Scala sources directly via zinc, for the target project's Scala version, collecting structured compiler
  * problems (used to roll back individual mutants that don't compile).
  *
  * @param scalaVersion
  *   full Scala version of the target project (e.g. `2.13.18`, `3.3.4`)
  * @param scalaCompilerClasspath
  *   the resolved Scala compiler jars (scala-compiler/scala3-compiler + library + reflect)
  * @param compilerBridgeJar
  *   the resolved, precompiled compiler bridge jar for that Scala version
  */
class ZincCompiler(scalaInstance: ScalaInstance, compilers: Compilers)(using Logger) {

  private val converter = PlainVirtualFileConverter.converter

  private val incCompiler = ZincUtil.defaultIncrementalCompiler

  /** Compile `sources` against `classpath` into `outputDir`. Returns the compiler error problems (empty on success).
    * Non-compile failures propagate as exceptions.
    */
  def compile(
      sources: Seq[Path],
      classpath: Seq[Path],
      outputDir: Path,
      scalacOptions: Seq[String],
      cacheFile: File
  ): IO[Seq[Problem]] = IO.defer {
    val sourceFiles: Array[VirtualFile] = sources.map(p => converter.toVirtualFile(p.toNioPath)).toArray
    val classpathFiles: Array[VirtualFile] =
      (scalaInstance.libraryJars.map(_.toPath()).toSeq ++ classpath.map(_.toNioPath)).distinct
        .map(converter.toVirtualFile)
        .toArray
    val reporter = new CollectingReporter

    val options = CompileOptions.of(
      classpathFiles,
      sourceFiles,
      outputDir.toNioPath,
      scalacOptions.toArray,
      Array.empty[String],
      100,
      (p: Position) => p,
      CompileOrder.Mixed,
      Optional.empty[java.nio.file.Path](),
      Optional.of[FileConverter](converter),
      Optional.empty[ReadStamps](),
      Optional.empty[Output]()
    )
    val lookup = new PerClasspathEntryLookup {
      override def analysis(classpathEntry: VirtualFile): Optional[CompileAnalysis] = Optional.empty()
      override def definesClass(classpathEntry: VirtualFile): DefinesClass =
        sbt.internal.inc.Locate.definesClass(classpathEntry)
    }
    val setup = Setup.of(
      lookup,
      false,
      cacheFile,
      CompilerCache.fresh(),
      IncOptions.of(),
      reporter,
      Optional.empty[CompileProgress](),
      Array.empty[T2[String, String]]
    )
    val previousResult = PreviousResult.of(Optional.empty[CompileAnalysis](), Optional.empty[MiniSetup]())
    val inputs = Inputs.of(compilers, options, setup, previousResult)

    IO.blocking(incCompiler.compile(inputs, XsbtiLogger()))
      .as(reporter.errors)
      .handleErrorWith {
        case failed: xsbti.CompileFailed =>
          // Errors were already logged to the reporter; include the exception's problems defensively
          IO.pure((reporter.errors ++ failed.problems().toSeq).distinct)
        case other => IO.raiseError(other)
      }
  }

}

object ZincCompiler {

  // must match the zinc version in pom.xml
  private val zincVersion = "2.0.0"

  def make(project: MavenProject, resolver: ArtifactResolver)(using log: Logger): Resource[IO, ZincCompiler] = {
    val scalaVersion = ScalaVersions.fullVersionUnsafe(project)

    val binaryVersion = ScalaVersions.binaryVersion(scalaVersion)
    val isScala3 = binaryVersion == "3"
    val compilerCoords =
      if isScala3 then s"org.scala-lang:scala3-compiler_3:$scalaVersion"
      else s"org.scala-lang:scala-compiler:$scalaVersion"
    val bridgeCoords =
      if isScala3 then s"org.scala-lang:scala3-sbt-bridge:$scalaVersion"
      else s"org.scala-sbt:compiler-bridge_$binaryVersion:$zincVersion"

    log.debug(s"Resolving Scala $scalaVersion compiler ($compilerCoords) and bridge ($bridgeCoords)")

    for {
      scalaInstance <- resolver
        .resolveTransitively(compilerCoords)
        .toResource
        .flatMap(makeScalaInstance(scalaVersion, _))
      scalac <- resolver.resolveArtifact(bridgeCoords).map(ZincUtil.scalaCompiler(scalaInstance, _)).toResource
      compilers = ZincUtil.compilers(scalaInstance, ClasspathOptionsUtil.boot(), None, scalac)
    } yield new ZincCompiler(scalaInstance, compilers)
  }

  def makeScalaInstance(scalaVersion: String, scalaCompilerClasspath: Seq[File]): Resource[IO, ScalaInstance] = {
    val allJars = scalaCompilerClasspath.filterNot { jar =>
      val name = jar.getName()
      name.startsWith("compiler-interface") || name.startsWith("util-interface")
    }.toArray
    val libraryJars = allJars.filter { jar =>
      val name = jar.getName()
      name.startsWith("scala-library") || name.startsWith("scala3-library") || name.startsWith("scala-reflect")
    }

    for {
      libraryLoader <- Resource.fromAutoCloseable(
        IO(new URLClassLoader(libraryJars.map(_.toURI().toURL()), xsbtiSharingLoader))
      )
      fullLoader <- Resource.fromAutoCloseable(IO(new URLClassLoader(allJars.map(_.toURI().toURL()), libraryLoader)))
    } yield new ScalaInstance(
      scalaVersion,
      fullLoader,
      fullLoader,
      libraryLoader,
      libraryJars,
      allJars,
      allJars,
      scalaVersion.some
    )
  }

  private def platformLoader: ClassLoader =
    // Java 9+: the platform classloader (no application classes). On Java 8 this is the ext loader.
    Option(ClassLoader.getSystemClassLoader()).map(_.getParent()).orNull

  /** A loader that exposes only the plugin realm's `xsbti.*` classes (the compiler-interface used by the embedded
    * zinc), so the freshly-loaded Scala compiler and bridge share that exact single copy.
    */
  private def xsbtiSharingLoader: ClassLoader = {
    val pluginLoader = getClass().getClassLoader()
    new ClassLoader(platformLoader) {
      override def findClass(name: String): Class[?] =
        if name.startsWith("xsbti.") then pluginLoader.loadClass(name)
        else throw new ClassNotFoundException(name)
    }
  }
}

/** Collects compiler problems reported during a zinc compile. */
private class CollectingReporter extends Reporter {
  private val buffer = ListBuffer.empty[Problem]

  def errors: Seq[Problem] = buffer.toSeq.filter(_.severity() == Severity.Error)

  override def reset(): Unit = buffer.clear()
  override def hasErrors(): Boolean = buffer.exists(_.severity() == Severity.Error)
  override def hasWarnings(): Boolean = buffer.exists(_.severity() == Severity.Warn)
  override def printSummary(): Unit = ()
  override def problems(): Array[Problem] = buffer.toArray
  override def log(problem: Problem): Unit = { buffer += problem; () }
  override def comment(pos: Position, msg: String): Unit = ()
}
