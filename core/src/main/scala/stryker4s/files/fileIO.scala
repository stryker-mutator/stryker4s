package stryker4s.files
import better.files._
import scala.concurrent.Future

sealed trait FileIO {
  def createAndWriteFromResource(file: File, resource: String): Future[Unit]

  def createAndWrite(file: File, content: String): Future[Unit]
}

object DiskFileIO extends FileIO {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  override def createAndWriteFromResource(file: File, resourceName: String): Future[Unit] =
    Future {
      file.createFileIfNotExists(createParents = true)

      for {
        in <- Resource.getAsStream(resourceName).autoClosed
        out <- file.newOutputStream.autoClosed
      } in pipeTo out
    }

  override def createAndWrite(file: File, content: String): Future[Unit] =
    Future {
      file.createFileIfNotExists(createParents = true)
      file.writeText(content)
      ()
    }
}
