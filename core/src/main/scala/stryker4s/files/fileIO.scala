package stryker4s.files
import java.nio.file.Path

import cats.effect.IO
import fs2._
import fs2.io.file._
import fs2.io.readInputStream

sealed trait FileIO {
  def createAndWriteFromResource(file: Path, resource: String): IO[Unit]

  def createAndWrite(file: Path, content: String): IO[Unit]
}

class DiskFileIO() extends FileIO {
  override def createAndWriteFromResource(file: Path, resourceName: String): IO[Unit] = {
    val stream = IO(getClass().getResourceAsStream(resourceName))

    Files[IO].createDirectories(file.getParent()) *>
      readInputStream(stream, 8192)
        .through(Files[IO].writeAll(file))
        .compile
        .drain
  }

  override def createAndWrite(file: Path, content: String): IO[Unit] = {
    Files[IO].createDirectories(file.getParent()) *>
      Stream(content)
        .through(text.utf8Encode)
        .through(Files[IO].writeAll(file))
        .compile
        .drain
  }
}
