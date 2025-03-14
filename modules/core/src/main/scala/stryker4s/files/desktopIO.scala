package stryker4s.files

import cats.effect.IO
import cats.syntax.parallel.*
import fs2.io.file.{Files, Path}

import java.awt.Desktop
import java.io.File
import scala.sys.process.*

trait DesktopIO {
  def attemptOpen(path: Path): IO[Unit]
}

class DesktopFileIO extends DesktopIO {
  override def attemptOpen(path: Path): IO[Unit] = {
    (isDesktopSupported, Files[IO].exists(path)).parTupled
      .flatMap { case (desktopSupported, fileExists) =>
        IO.whenA(desktopSupported && fileExists)(openFile(path.toNioPath.toFile()))
      }
  }

  def openFile(file: File): IO[Unit] =
    IO(sys.props.get("os.version").exists(_.contains("WSL"))).flatMap { isWsl =>
      if (isWsl)
        // Convert to windows-style path with wslpath utility and open with powershell
        IO.blocking(s"wslpath -ma $file".!!.trim())
          .flatMap(file => IO.blocking(s"powershell.exe -NoProfile -Command Start-Process file://$file".!!))
          .void
      else IO.blocking(Desktop.getDesktop.open(file))
    }

  def isDesktopSupported: IO[Boolean] = IO(
    Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.OPEN)
  )
}
