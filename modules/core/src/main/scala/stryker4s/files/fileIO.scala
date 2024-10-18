package stryker4s.files

import cats.effect.IO
import fs2.*
import fs2.io.file.*
import fs2.io.readInputStream

trait FileIO {
  def createAndWriteFromResource(file: Path, resource: String): IO[Unit]

  def createAndWrite(file: Path, content: String): IO[Unit]
}

class DiskFileIO() extends FileIO {
  override def createAndWriteFromResource(file: Path, resourceName: String): IO[Unit] = {
    val stream = IO(getClass().getResourceAsStream(resourceName))

    Files[IO].createDirectories(file.parent.get) *>
      readInputStream(stream, 8192)
        .through(Files[IO].writeAll(file))
        .compile
        .drain
  }

  override def createAndWrite(file: Path, content: String): IO[Unit] = {
    Files[IO].createDirectories(file.parent.get) *>
      Stream(content)
        .through(text.utf8.encode)
        .through(Files[IO].writeAll(file))
        .compile
        .drain
  }
}
