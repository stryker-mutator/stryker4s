package stryker4s.sbt.runner

import cats.effect.{IO, Resource}
import com.google.protobuf.CodedInputStream
import stryker4s.api.testprocess.{Request, Response, ResponseMessage}
import stryker4s.extension.IOExtensions._
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
      skipCancel(IO.blocking(request.asMessage.writeDelimitedTo(out))).logTimed("SendMessage") *>
      skipCancel(IO.blocking(ResponseMessage.parseDelimitedFrom(in))).logTimed("ReceiveMessage")
        .map(_.get)
        .map(_.toResponse)
        .flatTap(response => IO(log.debug(s"Received message $response")))

  /** Returns a new IO that instantly returns when cancelled, instead of calling it's cancellation logic
    *
    * This is needed because the blocking call only starts its cancellation logic when the blocking call returns, which
    * goes against what we want when e.g. a timeout occurs
    */
  private def skipCancel[T](f: IO[T]) = f.start.flatMap(_.joinWithNever)

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
