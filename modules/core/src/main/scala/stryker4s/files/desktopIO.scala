package stryker4s.files

import cats.effect.IO
import cats.syntax.parallel.*
import fs2.io.file.{Files, Path}
import fs2.io.process.{Process, ProcessBuilder}
import fs2.text.utf8

import java.awt.Desktop
import java.io.File

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
      if (isWsl) for {
        // Convert to windows-style path with wslpath utility and open with powershell
        file <- spawnProcess("wslpath", "-ma", file.toString()).use(processStdout)
        _ <- spawnProcess("powershell.exe", "-NoProfile", "-Command", "Start-Process", s"file://$file")
          .use(_.exitValue)
      } yield ()
      else IO.blocking(Desktop.getDesktop.open(file))
    }

  private def spawnProcess(command: String, args: String*) = ProcessBuilder(command, args.toList).spawn[IO]

  private def processStdout(p: Process[IO]): IO[String] = p.stdout.through(utf8.decode).compile.string.map(_.trim())

  def isDesktopSupported: IO[Boolean] = IO(
    Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.OPEN)
  )
}
