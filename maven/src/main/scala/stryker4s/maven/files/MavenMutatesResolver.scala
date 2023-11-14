package stryker4s.maven.files

import org.apache.maven.project.MavenProject
import stryker4s.files.MutatesFileResolver
import cats.effect.IO
import fs2.io.file.Path
import fs2.Stream
import fs2.io.file.Files
import scala.jdk.CollectionConverters.*
import stryker4s.files.Glob

class MavenMutatesResolver(project: MavenProject) extends MutatesFileResolver {

  override def files: fs2.Stream[IO, Path] =
    Stream
      .emits(project.getCompileSourceRoots().asScala)
      .map(Path(_))
      .evalFilter(Files[IO].exists)
      .flatMap(Glob.glob(_, Seq("**/*.scala")))

}
