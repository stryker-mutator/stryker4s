package stryker4jvm.plugin.sbt.runner

import cats.effect.IO
import cats.syntax.bifunctor.*
import com.google.protobuf.UInt32Value
import fs2.Chunk
import fs2.io.net.Socket
import scodec.bits.BitVector
import stryker4jvm.logging.FansiLogger
import stryker4s.api.testprocess.{Request, RequestMessage, Response, ResponseMessage}

sealed trait TestRunnerConnection {
  def sendMessage(request: Request): IO[Response]
}

final class SocketTestRunnerConnection(socket: Socket[IO])(implicit log: FansiLogger) extends TestRunnerConnection {

  override def sendMessage(request: Request): IO[Response] =
    IO(log.debug(s"Sending message $request")) *>
      (write(request.asMessage) *> read)
        .flatTap(response => IO(log.debug(s"Received message $response")))

  def write(msg: RequestMessage) = {
    // Delimiter announcing the size of the upcoming message. `tail` because the first byte is the tag of the message, which isn't included in the delimiter (it is always uint32)
    val serializedSize = msg.serializedSize
    val delimiter = Chunk.array(UInt32Value.newBuilder.setValue(serializedSize).build.toByteArray().tail)

    IO(log.debug(s"Writing message of $serializedSize bytes")) *>
      socket.write(delimiter ++ Chunk.array(msg.toByteArray))
  }

  def read: IO[Response] = {
    // The delimiter tells us how many bytes to read for the response.
    // @see https://developers.google.com/protocol-buffers/docs/encoding#varints
    def readDelimiter(acc: BitVector): IO[BitVector] =
      socket
        .readN(1)
        // If the first bit is positive (1), continue reading bytes
        .map(_.toBitVector.splitAt(1).leftMap(_.head))
        .flatMap { case (continue, newBits) =>
          val sum = newBits ++ acc
          if (continue) readDelimiter(sum)
          else IO.pure(sum)
        }

    readDelimiter(BitVector.empty)
      .map(_.toInt(false))
      .flatTap(size => IO(log.debug(s"Reading message of $size bytes")))
      .flatMap(socket.readN)
      .map(bytes => ResponseMessage.parseFrom(bytes.toArray).toResponse)
  }

}
