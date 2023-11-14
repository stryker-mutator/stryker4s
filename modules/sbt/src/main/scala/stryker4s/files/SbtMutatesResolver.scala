package stryker4s.files

import cats.effect.IO
import fs2.Stream
import fs2.io.file.Path
import sbt.Keys.sources
import sbt.{Compile, Project, State, Value}
import stryker4s.exception.TestSetupException

class SbtMutatesResolver(state: State, target: Path) extends MutatesFileResolver {

  def files: Stream[IO, Path] = Project.runTask(Compile / sources, state) match {
    case Some((_, Value(result))) if result.nonEmpty =>
      Stream.emits(result).map(f => Path.fromNioPath(f.toPath())).filterNot(_.startsWith(target))

    case _ => Stream.raiseError[IO](new TestSetupException("sources"))
  }

}
