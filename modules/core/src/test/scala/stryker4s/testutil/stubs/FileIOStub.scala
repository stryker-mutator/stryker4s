package stryker4s.testutil.stubs

import cats.effect.{IO, Ref}
import fs2.io.file.Path
import stryker4s.files.FileIO

trait FileIOStub extends FileIO {
  def createAndWriteFromResourceCalls: IO[Seq[(Path, String)]]

  def createAndWriteCalls: IO[Seq[(Path, String)]]
}

object FileIOStub {
  def apply(): FileIOStub = {
    val createAndWriteFromResourceRef = Ref.unsafe[IO, Seq[(Path, String)]](Seq.empty)
    val createAndWriteRef = Ref.unsafe[IO, Seq[(Path, String)]](Seq.empty)

    new FileIOStub {
      def createAndWriteFromResourceCalls: IO[Seq[(Path, String)]] = createAndWriteFromResourceRef.get

      def createAndWriteCalls: IO[Seq[(Path, String)]] = createAndWriteRef.get

      override def createAndWriteFromResource(file: Path, resource: String): IO[Unit] = {
        val t = (file, resource)
        createAndWriteFromResourceRef.update(_ :+ t)
      }
      override def createAndWrite(file: Path, content: String): IO[Unit] = {
        val t = (file, content)
        createAndWriteRef.update(_ :+ t)
      }
    }
  }
}
