package stryker4s.files
import cats.effect.IO
import fs2._
import fs2.io.readInputStream
import fs2.io.file._
import cats.effect.Blocker
import cats.effect.ContextShift
import cats.effect.Sync
import java.nio.file.Path
sealed trait FileIO {
  def createAndWriteFromResource(file: Path, resource: String): IO[Unit]

  def createAndWrite(file: Path, content: String): IO[Unit]
}

class DiskFileIO()(implicit cs: ContextShift[IO], s: Sync[IO]) extends FileIO {
  override def createAndWriteFromResource(file: Path, resourceName: String): IO[Unit] =
    Blocker[IO].use { blocker =>
      val stream = IO { this.getClass().getResourceAsStream(resourceName) }

      createDirectories(blocker, file.getParent()) *>
        readInputStream(stream, 8192, blocker)
          .through(writeAll(file, blocker))
          .compile
          .drain
    }

  override def createAndWrite(file: Path, content: String): IO[Unit] =
    Blocker[IO].use { blocker =>
      createDirectories(blocker, file.getParent()) *>
        Stream(content)
          .through(text.utf8Encode)
          .through(writeAll(file, blocker))
          .compile
          .drain
    }
}
