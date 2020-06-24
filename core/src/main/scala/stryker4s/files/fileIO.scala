package stryker4s.files
import better.files._
import cats.effect.IO

sealed trait FileIO {
  def createAndWriteFromResource(file: File, resource: String): IO[Unit]

  def createAndWrite(file: File, content: String): IO[Unit]
}

class DiskFileIO() extends FileIO {

  // TODO: Replace with fs2 writing
  override def createAndWriteFromResource(file: File, resourceName: String): IO[Unit] =
    IO {
      file.createFileIfNotExists(createParents = true)

      for {
        in <- Resource.getAsStream(resourceName).autoClosed
        out <- file.newOutputStream.autoClosed
      } in pipeTo out
    }

  override def createAndWrite(file: File, content: String): IO[Unit] =
    IO {
      file.createFileIfNotExists(createParents = true)
      file.writeText(content)
      ()
    }
}
