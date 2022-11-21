package stryker4jvm.scalatest

import fs2.io.file.Path

import java.io.FileNotFoundException
import java.nio.file

object FileUtil {
  private lazy val classLoader = getClass.getClassLoader

  def getResource(name: String): Path =
    Option(classLoader.getResource(name))
      .map(_.toURI())
      .map(file.Path.of)
      .map(Path.fromNioPath)
      .getOrElse(throw new FileNotFoundException(s"File $name could not be found"))
}
