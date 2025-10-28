package stryker4s.testutil.stubs

import cats.effect.{IO, Ref}
import fs2.Stream
import fs2.io.file.Path
import stryker4s.files.FileIO

trait FileIOStub extends FileIO {
  def resourceAsStreamCalls: IO[Seq[String]]

  def createAndWriteCalls: IO[Seq[(Path, String)]]
}

object FileIOStub {
  def apply(): FileIOStub = {
    val resourceAsStreamRef = Ref.unsafe[IO, Seq[String]](Seq.empty)
    val createAndWriteRef = Ref.unsafe[IO, Seq[(Path, String)]](Seq.empty)

    new FileIOStub {

      def resourceAsStreamCalls: IO[Seq[String]] = resourceAsStreamRef.get

      def createAndWriteCalls: IO[Seq[(Path, String)]] = createAndWriteRef.get

      override def resourceAsStream(resourceName: String): fs2.Stream[IO, Byte] =
        Stream.eval(resourceAsStreamRef.update(_ :+ resourceName)).drain

      override def createAndWrite(file: Path, content: fs2.Stream[IO, Byte]): IO[Unit] = for {
        contentStr <- content.through(fs2.text.utf8.decode).compile.string
        t = (file, contentStr)
        _ <- createAndWriteRef.update(_ :+ t)
      } yield ()
    }
  }
}
