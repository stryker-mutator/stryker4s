package stryker4s.files

import cats.effect.IO
import java.awt.Desktop
import java.io.File

trait DesktopIO {
  def open(file: java.io.File): IO[Either[String, Unit]]
}

class DesktopFileIO extends DesktopIO {
  override def open(file: File): IO[Either[String, Unit]] = {
    for {
      desktopResult <- desktopSupported
      result <- desktopResult match {
        case Left(errorMessage) => IO.pure(Left(errorMessage))
        case Right(_) =>
          fileExists(file).flatMap {
            case Left(errorMessage) => IO.pure(Left(errorMessage))
            case Right(_) =>
              openFile(file)
          }
      }
    } yield result
  }

  def openFile(file: File): IO[Either[String, Unit]] =
    IO(Desktop.getDesktop.open(file)).as(Right(())).handleError(e => Left(s"Error opening file: ${e.getMessage}"))

  def desktopSupported: IO[Either[String, Unit]] = IO {
    if (Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.OPEN))
      Right(())
    else
      Left("Desktop API is not supported")
  }

  def fileExists(file: File): IO[Either[String, Unit]] = IO {
    if (file.exists())
      Right(())
    else
      Left(s"File does not exist: ${file.getAbsolutePath}")
  }
}
