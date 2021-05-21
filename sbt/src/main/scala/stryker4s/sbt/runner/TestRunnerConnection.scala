package stryker4s.sbt.runner

import cats.effect.{IO, Resource}
import stryker4s.api.testprocess.{Request, Response}
import stryker4s.extension.exception.MutationRunFailedException
import stryker4s.log.Logger

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.Socket

sealed trait TestRunnerConnection {
  def sendMessage(request: Request): IO[Response]
}

final class SocketTestRunnerConnection(out: ObjectOutputStream, in: ObjectInputStream)(implicit log: Logger)
    extends TestRunnerConnection {

  override def sendMessage(request: Request): IO[Response] = {
    IO(log.debug(s"Sending message $request")) *>
      skipCancel(IO.blocking(out.writeObject(request))) *>
      skipCancel(IO.blocking(in.readObject())) flatMap {
        case response: Response =>
          IO(log.debug(s"Received message $response"))
            .as(response)
        case other =>
          IO.raiseError(
            new MutationRunFailedException(
              s"Expected an object of type 'Response' from socket connection, but received $other"
            )
          )
      }
  }

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
      out <- Resource.fromAutoCloseable(IO(new ObjectOutputStream(socket.getOutputStream())))
      in <- Resource.fromAutoCloseable(IO(new ObjectInputStream(socket.getInputStream())))
    } yield new SocketTestRunnerConnection(out, in)

}
