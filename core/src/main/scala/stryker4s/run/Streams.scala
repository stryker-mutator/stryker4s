package stryker4s.run

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import fs2.io
import fs2.text
import cats.effect.Sync
import stryker4s.config.{Config, ConfigReader}
import java.nio.file.Path
import cats.effect.ContextShift
import fs2.Stream
import fs2.Pipe
import scala.meta.Source
import scala.meta.parsers.XtensionParseInputLike
import scala.meta.inputs.Input.VirtualFile
import stryker4s.model.Mutant
import stryker4s.mutants.findmutants.MutantMatcher
import java.nio.file.PathMatcher
import cats.implicits._

trait Streams extends IOApp {

  def converter[F[_]: Sync: ContextShift] = Stream.eval(resolveConfig).flatMap { implicit config =>
    filesToMutate
      .through(readFiles)
      .through(parseTrees)
      .through(findMutations)
  }

  def run(args: List[String]): IO[ExitCode] = converter[IO].compile.drain.as(ExitCode.Success)

  def resolveConfig[F[_]: Sync]: F[Config]

  def filesToMutate[F[_]: Sync: ContextShift](implicit config: Config): Stream[F, Path]

  def readFiles[F[_]: Sync: ContextShift]: Pipe[F, Path, VirtualFile]

  def parseTrees[F[_]]: Pipe[F, VirtualFile, Source]

  def findMutations[F[_]](implicit config: Config): Pipe[F, Source, Stream[F, Mutant]]

}

object StreamsImpl extends Streams {
  override def resolveConfig[F[_]: Sync]: F[Config] =
    Sync[F].delay {
      ConfigReader.readConfig()
    }

  override def filesToMutate[F[_]: Sync: ContextShift](implicit config: Config): Stream[F, Path] = {
    val mutateGlobsFilter = globsFilter(config.mutate, config.baseDir.path)
    for {
      blocker <- Stream.resource(Blocker[F])
      file <- io.file
        .directoryStream(blocker, config.baseDir.path, mutateGlobsFilter)
    } yield file
  }

  def globsFilter(globs: Seq[String], baseDir: Path): Path => Boolean = {
    val (excludeDirty, include) = globs.partition(_.startsWith("!"))
    val exclude = excludeDirty.map(_.replaceFirst("!", ""))
    val fs = baseDir.getFileSystem()
    val includeMatchers = include.map(glob => fs.getPathMatcher(s"glob:$glob"))
    val excludeMatchers = exclude.map(glob => fs.getPathMatcher(s"glob:$glob"))
    val matcher = new PathMatcher() {
      override def matches(path: Path): Boolean = {
        val relativePath = baseDir.relativize(path)
        includeMatchers.exists(_.matches(relativePath)) && !excludeMatchers.exists(_.matches(relativePath))
      }
    }
    path => matcher.matches(path)
  }

  override def readFiles[F[_]: Sync: ContextShift]: Pipe[F, Path, VirtualFile] =
    paths =>
      for {
        blocker <- Stream.resource(Blocker[F])
        file <- paths
        fileStream = io.file.readAll(file, blocker, 8)
        fileContent <- fileStream.through(text.utf8Decode)
      } yield VirtualFile(file.toString, fileContent)

  override def parseTrees[F[_]]: fs2.Pipe[F, VirtualFile, Source] = _.map(s => s.parse[Source].get)

  override def findMutations[F[_]](implicit config: Config): fs2.Pipe[F, Source, Stream[F, Mutant]] =
    _.map(findMutationsInSource)

  def findMutationsInSource[F[_]](source: Source)(implicit config: Config): Stream[F, Mutant] = {
    import fs2._
    val matcher = new MutantMatcher()
    val (included, _) = source.collect(matcher.allMatchers).flatten.partition(_.isDefined)
    Stream.emits(included.flatten)
  }
}
