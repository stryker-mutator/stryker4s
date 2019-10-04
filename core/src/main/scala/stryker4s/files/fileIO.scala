package stryker4s.files
import better.files._

trait FileIO {
  def createAndWriteFromResource(file: File, resource: String)

  def createAndWrite(file: File, content: String): Unit
}

object DiskFileIO extends FileIO {

  override def createAndWriteFromResource(file: File, resourceName: String): Unit = {
    file.createFileIfNotExists(createParents = true)

    for {
      in <- getClass.getClassLoader.getResourceAsStream(resourceName).autoClosed
      out <- file.newOutputStream.autoClosed
    } in pipeTo out
  }

  override def createAndWrite(file: File, content: String): Unit = {
    file.createFileIfNotExists(createParents = true)
    file.writeText(content)
  }
}
