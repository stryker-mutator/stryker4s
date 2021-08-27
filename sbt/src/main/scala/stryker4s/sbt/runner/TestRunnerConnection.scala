package stryker4s.sbt.runner

import cats.effect.{IO, Resource}
import com.google.protobuf.CodedInputStream
import stryker4s.api.testprocess.{Request, Response, ResponseMessage}
import stryker4s.log.Logger

import java.io.OutputStream
import java.net.Socket

sealed trait TestRunnerConnection {
  def sendMessage(request: Request): IO[Response]
}

final class SocketTestRunnerConnection(out: OutputStream, in: CodedInputStream)(implicit log: Logger)
    extends TestRunnerConnection {

  override def sendMessage(request: Request): IO[Response] =
    IO(log.debug(s"Sending message $request")) *>
      IO.interruptible(false)(request.asMessage.writeDelimitedTo(out)) *>
      IO.interruptible(false)(ResponseMessage.parseDelimitedFrom(in))
        .map(_.get)
        .map(_.toResponse)
        .flatTap { response =>
          IO(log.debug(s"Received message $response"))
        }

}

object TestRunnerConnection {
  def create(socket: Socket)(implicit log: Logger): Resource[IO, TestRunnerConnection] =
    for {
      out <- Resource.fromAutoCloseable(IO(socket.getOutputStream()))
      in <- Resource
        .fromAutoCloseable(IO(socket.getInputStream()))
        .evalMap(inStr => IO(CodedInputStream.newInstance(inStr)))
    } yield new SocketTestRunnerConnection(out, in)

}
