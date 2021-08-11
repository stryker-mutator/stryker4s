package stryker4s.files

import cats.effect.IO
import fs2.Stream
import fs2.io.file.{Files, Path}
import stryker4s.extension.StreamExtensions._

class SbtFilesResolver(sources: Seq[Path], target: Path) extends FilesFileResolver {

  def files: fs2.Stream[IO, Path] = {
    val s = Stream
      .emits(sources)
      .evalFilter(Files[IO].exists)
      .flatMap(Files[IO].walk)
      .filterNot(_.startsWith(target))
      .evalFilterNot(Files[IO].isDirectory)

    val distinct = s.compile.toVector.map(_.distinct)

    Stream.evals(distinct)
  }
}
