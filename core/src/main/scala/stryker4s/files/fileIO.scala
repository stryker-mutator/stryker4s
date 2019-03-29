package stryker4s.files
import better.files.{File, Resource}

trait FileIO {
  def readResource(resource: String): String

  def createAndWrite(file: File, content: String): Unit
}

object DiskFileIO extends FileIO {

  override def createAndWrite(file: File, content: String): Unit = {
    file.createIfNotExists(asDirectory = false, createParents = true)
    file.write(content)
  }
  override def readResource(resource: String): String = Resource.getAsString(resource)
}
