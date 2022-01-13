package stryker4s.sbt.runner

import cats.effect.IO
import com.google.protobuf.UInt32Value
import fs2.Chunk
import fs2.io.net.Socket
import scodec.bits.BitVector
import stryker4s.api.testprocess.{Request, RequestMessage, Response, ResponseMessage}
import stryker4s.log.Logger

sealed trait TestRunnerConnection {
  def sendMessage(request: Request): IO[Response]
}

final class SocketTestRunnerConnection(socket: Socket[IO])(implicit log: Logger) extends TestRunnerConnection {

  override def sendMessage(request: Request): IO[Response] =
    IO(log.debug(s"Sending message $request")) *>
      write(request.asMessage) *>
      read
        .flatTap(response => IO(log.debug(s"Received message $response")))

  def write(msg: RequestMessage) = {
    // Delimiter announcing the size of the upcoming message. `tail` because the first byte is the tag of the message, which isn't included in the delimiter (it is always uint32)
    val serializedSize = msg.serializedSize
    val delimiter = Chunk.array(UInt32Value.of(serializedSize).toByteArray().tail)

    IO(log.debug(s"Writing message of $serializedSize bytes")) *>
      socket.write(delimiter ++ Chunk.array(msg.toByteArray))
  }

  def read: IO[Response] = {
    // The delimiter tells us how many bytes to read for the response.
    // @see https://developers.google.com/protocol-buffers/docs/encoding#varints
    def readDelimiter: IO[BitVector] =
      socket
        .readN(1)
        .map(_.toBitVector)
        .map(_.splitAt(1))
        .flatMap { case (continue, sum) =>
          // If the first bit is positive (1), continue reading bytes
          if (continue.head) readDelimiter.map(_ ++ sum) // Construct all bits appending new reads to the _start_
          else IO.pure(sum)
        }

    readDelimiter
      .map(_.toInt(false))
      .flatTap(size => IO(log.debug(s"Reading message of $size bytes")))
      .flatMap(socket.readN)
      .map(bytes => ResponseMessage.parseFrom(bytes.toArray).toResponse)
  }

}
