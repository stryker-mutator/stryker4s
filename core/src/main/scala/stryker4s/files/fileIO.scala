package stryker4s.files
import better.files._
import cats.effect.IO
import fs2._
import fs2.io.readInputStream
import fs2.io.file._
import cats.effect.Blocker
import cats.effect.ContextShift
import cats.effect.Sync
sealed trait FileIO {
  def createAndWriteFromResource(file: File, resource: String): IO[Unit]

  def createAndWrite(file: File, content: String): IO[Unit]
}

class DiskFileIO()(implicit cs: ContextShift[IO], s: Sync[IO]) extends FileIO {
  override def createAndWriteFromResource(file: File, resourceName: String): IO[Unit] =
    Blocker[IO].use { blocker =>
      val stream = IO { this.getClass().getResourceAsStream(resourceName) }

      readInputStream(stream, 8192, blocker)
        .through(writeAll(file.path, blocker))
        .compile
        .drain
    }

  override def createAndWrite(file: File, content: String): IO[Unit] =
    Blocker[IO].use { blocker =>
      createDirectories(blocker, file.parent.path).flatMap { _ =>
        Stream(content)
          .through(text.utf8Encode)
          .through(writeAll(file.path, blocker))
          .compile
          .drain
      }
    }
}
