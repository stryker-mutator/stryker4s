package stryker4jvm.plugin.sbt.files

import cats.effect.IO
import fs2.Stream
import fs2.io.file.Path
import sbt.Keys.sources
import sbt.{Compile, Project, State, Value}
import stryker4jvm.exception.TestSetupException
import stryker4jvm.extensions.StreamExtensions.*
import stryker4jvm.files.MutatesFileResolver

class SbtMutatesResolver(state: State, target: Path) extends MutatesFileResolver {

  def files: Stream[IO, Path] = Project.runTask(Compile / sources, state) match {
    case Some((_, Value(result))) if result.nonEmpty =>
      Stream.emits(result).map(f => Path.fromNioPath(f.toPath())).filterNot(_.startsWith(target))

    case _ => Stream.raiseError[IO](new TestSetupException("sources"))
  }

}
