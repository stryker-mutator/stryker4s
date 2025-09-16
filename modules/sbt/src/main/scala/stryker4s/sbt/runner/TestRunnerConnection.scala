package stryker4s.sbt.runner

import cats.effect.{IO, Resource}
import com.comcast.ip4s.{GenSocketAddress, UnixSocketAddress}
import com.google.protobuf.CodedOutputStream
import fs2.io.file.{Files, Path}
import fs2.io.net.{Network, Socket}
import fs2.io.{readOutputStream, toInputStreamResource}
import scalapb.LiteParser
import stryker4s.log.Logger
import stryker4s.testrunner.api.{Request, RequestMessage, Response, ResponseMessage}

import java.io.InputStream
import java.net.SocketException

sealed trait TestRunnerConnection {
  def sendMessage(request: Request): IO[Response]
}

final class SocketTestRunnerConnection private (socket: Socket[IO], input: InputStream)(implicit log: Logger)
    extends TestRunnerConnection {

  override def sendMessage(request: Request): IO[Response] =
    (write(request.asMessage) *> read)
      .flatTap(response => IO(log.debug(s"Received message $response")))

  def write(msg: RequestMessage) =
    readOutputStream(bufferSizeForMsg(msg))(os => IO.blocking(msg.writeDelimitedTo(os)))
      .through(socket.writes)
      .compile
      .drain

  def read: IO[Response] = IO.blocking(ResponseMessage.parseDelimitedFrom(input)).flatMap {
    case Some(responseMsg) => IO.pure(responseMsg.toResponse)
    case None              => IO.raiseError(new RuntimeException("Failed to parse ResponseMessage from input stream"))
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
    input <- toInputStreamResource(socket.reads)
  } yield new SocketTestRunnerConnection(socket, input)

}
