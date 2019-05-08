package stryker4s.files
import better.files.File

import scala.io.Source

trait FileIO {
  def readResource(resource: String): Source

  def createAndWrite(file: File, content: Iterator[Char]): Unit

  def createAndWrite(file: File, content: String): Unit
}

object DiskFileIO extends FileIO {
  override def readResource(resource: String): Source = {
    val stream = getClass.getClassLoader.getResourceAsStream(resource)
    Source.fromInputStream(stream)
  }

  override def createAndWrite(file: File, content: Iterator[Char]): Unit = {
    file.createFileIfNotExists(createParents = true)
    file.writeBytes(content.map(_.toByte))
  }

  override def createAndWrite(file: File, content: String): Unit = {
    file.createFileIfNotExists(createParents = true)
    file.writeText(content)
  }
}
