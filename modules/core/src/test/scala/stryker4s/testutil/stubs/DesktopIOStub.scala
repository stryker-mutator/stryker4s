package stryker4s.testutil.stubs

import stryker4s.files.DesktopIO
import cats.effect.{IO, Ref}

trait DesktopIOStub extends DesktopIO {
  def openCalls: IO[Seq[java.io.File]]
}

object DesktopIOStub {
  def apply(): DesktopIOStub = {
    val openRef = Ref.unsafe[IO, Seq[java.io.File]](Seq.empty)

    new DesktopIOStub {
      def openCalls: IO[Seq[java.io.File]] = openRef.get

      override def open(file: java.io.File): IO[Either[String, Unit]] = {
        openRef.update(_ :+ file).as(Right(()))
      }
    }
  }
}
