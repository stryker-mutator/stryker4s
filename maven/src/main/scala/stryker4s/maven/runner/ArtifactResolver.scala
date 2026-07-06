package stryker4s.maven.runner

import cats.effect.IO
import fs2.io.file.Path
import org.apache.maven.execution.MavenSession
import org.apache.maven.project.MavenProject
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.resolution.{ArtifactRequest, DependencyRequest}
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.filter.DependencyFilterUtils
import stryker4s.log.Logger

import scala.jdk.CollectionConverters.*

/** Resolves Maven artifacts (and their transitive runtime dependencies) at plugin runtime via Aether, against the
  * target project's configured repositories. Used to fetch the `stryker4s-testrunner` artifact and the Scala compiler +
  * bridge for the project's Scala version, none of which the plugin can know at build time.
  */
class ArtifactResolver(project: MavenProject, session: MavenSession, repoSystem: RepositorySystem)(using log: Logger) {
  private val repositories = project.getRemoteProjectRepositories()
  private val repoSession = session.getRepositorySession()

  /** Resolve a single artifact, without its transitive dependencies.
    *
    * @param coordinates
    *   `groupId:artifactId[:extension[:classifier]]:version`
    */
  def resolveArtifact(coordinates: String): IO[Path] = {
    log.debug(s"Resolving artifact $coordinates")
    val request = new ArtifactRequest(new DefaultArtifact(coordinates), repositories, null)
    IO.blocking:
      Path.fromNioPath(repoSystem.resolveArtifact(repoSession, request).getArtifact().getPath())
  }

  /** Resolve an artifact together with all of its transitive runtime dependencies. */
  def resolveTransitively(coordinates: String): IO[Seq[Path]] = {
    log.debug(s"Resolving $coordinates with transitive dependencies")
    val root = new Dependency(new DefaultArtifact(coordinates), JavaScopes.RUNTIME)
    val collectRequest = new CollectRequest(root, repositories)
    val filter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME)
    val dependencyRequest = new DependencyRequest(collectRequest, filter)

    IO.blocking:
      repoSystem
        .resolveDependencies(repoSession, dependencyRequest)
        .getArtifactResults()
        .asScala
        .toSeq
        .map(a => Path.fromNioPath(a.getArtifact().getPath()))
  }
}
