package stryker4s.files
import better.files.File

import scala.io.Source

trait FileIO {
  def readResource(resource: String): Source

  def createAndWrite(file: File, content: Iterator[Char]): Unit
}

object DiskFileIO extends FileIO {

  override def createAndWrite(file: File, content: Iterator[Char]): Unit = {
    file.createIfNotExists(asDirectory = false, createParents = true)
    file.writeBytes(content.map(_.toByte))
  }
  override def readResource(resource: String): Source = {
    val stream = getClass.getClassLoader.getResourceAsStream(resource)
    Source.fromInputStream(stream)
  }
}
