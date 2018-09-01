package stryker4s.scalatest

import java.io.FileNotFoundException

import better.files.File

object FileUtil {
  private val classLoader = getClass.getClassLoader

  def getResource(name: String): File = {
    val resource = Option(classLoader.getResource(name))
    resource
      .map(File(_))
      .getOrElse(throw new FileNotFoundException(s"File $name could not be found"))
  }
}
