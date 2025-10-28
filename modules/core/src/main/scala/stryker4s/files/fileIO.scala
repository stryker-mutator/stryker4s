package stryker4s.files

import cats.effect.IO
import fs2.io.file.{Files, Path}
import fs2.*

import java.io.IOException

trait FileIO {

  def resourceAsStream(resourceName: String): Stream[IO, Byte]

  def createAndWrite(file: Path, content: Stream[IO, Byte]): IO[Unit]

}

class DiskFileIO() extends FileIO {
  override def resourceAsStream(name: String): Stream[IO, Byte] =
    Stream.eval(IO.blocking(Option(getClass.getResourceAsStream(name)))).flatMap {
      case Some(resource) => io.readInputStream(IO.pure(resource), 8192)
      case None           => Stream.raiseError[IO](new IOException(s"Resource $name not found"))
    }

  override def createAndWrite(file: Path, content: Stream[IO, Byte]): IO[Unit] = {
    Files[IO].createDirectories(file.parent.get) *>
      content
        .through(Files[IO].writeAll(file))
        .compile
        .drain
  }
}
