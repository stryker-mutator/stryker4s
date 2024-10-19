package stryker4s.files

import cats.effect.IO
import java.awt.Desktop
import java.io.File
import fs2.io.file.Files
import fs2.io.file.Path

trait DesktopIO {
  def attemptOpen(path: Path): IO[Unit]
}

class DesktopFileIO extends DesktopIO {
  override def attemptOpen(path: Path): IO[Unit] =
    isDesktopSupported.ifM(
      Files[IO]
        .exists(path)
        .ifM(
          openFile(path.toNioPath.toFile()),
          IO.unit
        ),
      IO.unit
    )

  def openFile(file: File): IO[Unit] = IO(Desktop.getDesktop.open(file))

  def isDesktopSupported: IO[Boolean] = IO(
    Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.OPEN)
  )
}
