package stryker4s.scalatest

import java.io.FileNotFoundException

import better.files.File

object FileUtil {
  private def classLoader = getClass.getClassLoader

  def getResource(name: String): File =
    Option(classLoader.getResource(name))
      .map(File(_))
      .getOrElse(throw new FileNotFoundException(s"File $name could not be found"))
}
