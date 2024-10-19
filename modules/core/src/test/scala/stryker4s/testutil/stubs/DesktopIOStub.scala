package stryker4s.testutil.stubs

import stryker4s.files.DesktopIO
import cats.effect.{IO, Ref}
import fs2.io.file.Path

trait DesktopIOStub extends DesktopIO {
  def openCalls: IO[Seq[Path]]
}

object DesktopIOStub {
  def apply(): DesktopIOStub = {
    val openRef = Ref.unsafe[IO, Seq[Path]](Seq.empty)

    new DesktopIOStub {
      def openCalls: IO[Seq[Path]] = openRef.get

      override def attemptOpen(path: Path): IO[Unit] = openRef.update(_ :+ path)
    }
  }
}
