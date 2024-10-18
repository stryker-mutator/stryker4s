package stryker4s.files

import cats.effect.IO
import fs2.Stream
import fs2.io.file.{Files, Path}
import stryker4s.config.Config
import stryker4s.files.Glob.glob

class GlobFileResolver protected[files] (basePath: Path, pattern: Seq[String]) extends FileResolver {

  override def files: Stream[IO, Path] =
    glob(basePath, pattern)
      .evalFilterNot(Files[IO].isDirectory)

}

object GlobFileResolver {
  def forMutate()(implicit config: Config): GlobFileResolver = new GlobFileResolver(config.baseDir, config.mutate)
  def forFiles()(implicit config: Config): GlobFileResolver = new GlobFileResolver(config.baseDir, config.files)
}
