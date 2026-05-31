package stryker4s.sbt.runner

import cats.effect.{IO, Resource}
import com.comcast.ip4s.{GenSocketAddress, UnixSocketAddress}
import com.google.protobuf.{CodedInputStream, CodedOutputStream}
import fs2.Chunk
import fs2.io.file.{Files, Path}
import fs2.io.net.{Network, Socket}
import fs2.io.readOutputStream
import scalapb.LiteParser
import stryker4s.log.Logger
import stryker4s.testrunner.api.{Request, RequestMessage, Response, ResponseMessage}

import java.net.SocketException

sealed trait TestRunnerConnection {
  def sendMessage(request: Request): IO[Response]
}

final class SocketTestRunnerConnection private (socket: Socket[IO])(implicit log: Logger) extends TestRunnerConnection {

  override def sendMessage(request: Request): IO[Response] =
    (write(request.asMessage) *> read)
      .flatTap(response => IO(log.debug(s"Received message $response")))

  def write(msg: RequestMessage) =
    readOutputStream(bufferSizeForMsg(msg))(os => IO.blocking(msg.writeDelimitedTo(os)))
      .through(socket.writes)
      .compile
      .drain

  def read: IO[Response] =
    readVarint32
      .flatMap(readExactly)
      .map(bytes => ResponseMessage.parseFrom(bytes.toArray).toResponse)

  private def readVarint32: IO[Int] = {
    def readWhileContinuationBitSet(readSoFar: Chunk[Byte]): IO[Chunk[Byte]] =
      readByte.flatMap { byte =>
        val read = readSoFar ++ Chunk(byte)
        if ((byte & 0x80) != 0) readWhileContinuationBitSet(read) else IO.pure(read)
      }

    readWhileContinuationBitSet(Chunk.empty).map(bytes => CodedInputStream.newInstance(bytes.toArray).readRawVarint32())
  }

  private def readByte: IO[Byte] = readExactly(1).map(_(0))

  private def readExactly(numBytes: Int): IO[Chunk[Byte]] =
    socket.readN(numBytes).flatMap { chunk =>
      if (chunk.size == numBytes) IO.pure(chunk)
      else IO.raiseError(new SocketException("Test-runner closed the connection while reading a message"))
    }

  /** Copied from RequestMessage#writeDelimitedTo
    */
  private def bufferSizeForMsg(msg: RequestMessage): Int = {
    val serialized = msg.serializedSize
    LiteParser.preferredCodedOutputStreamBufferSize(
      CodedOutputStream.computeUInt32SizeNoTag(serialized) + serialized
    )
  }

}

object SocketTestRunnerConnection {

  def create(socketAddress: GenSocketAddress)(implicit log: Logger): Resource[IO, TestRunnerConnection] = for {
    _ <- socketAddress match {
      case UnixSocketAddress(path) =>
        Files[IO]
          .exists(Path(path))
          .ifM(IO.unit, IO.raiseError(new SocketException(s"Socket file $path does not exist")))
          .toResource
      case _ => Resource.unit[IO]
    }
    socket <- Network[IO].connect(socketAddress)
  } yield new SocketTestRunnerConnection(socket)

}
