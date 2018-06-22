package stryker4s.scalatest

import better.files.File

object FileUtil {
  private val classLoader = getClass.getClassLoader

  def getResource(name: String): File = File(classLoader.getResource(name))
}
