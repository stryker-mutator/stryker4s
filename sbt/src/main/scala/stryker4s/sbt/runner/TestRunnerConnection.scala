package stryker4s.sbt.runner

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.Socket

import cats.effect.{Blocker, ContextShift, IO, Resource}
import grizzled.slf4j.Logging
import stryker4s.api.testprocess.{Request, Response}
import stryker4s.extension.exception.MutationRunFailedException

sealed trait TestRunnerConnection {
  def sendMessage(request: Request): IO[Response]
}

final class SocketTestRunnerConnection(blocker: Blocker, out: ObjectOutputStream, in: ObjectInputStream)(implicit
    cs: ContextShift[IO]
) extends TestRunnerConnection
    with Logging {

  override def sendMessage(request: Request): IO[Response] = {
    IO(debug(s"Sending message $request")) *>
      blocker.delay[IO, Unit](out.writeObject(request)) *>
      blocker.delay[IO, Any](in.readObject()) flatMap {
      case response: Response =>
        IO {
          debug(s"Received message $response")
          response
        }
      case other =>
        IO.raiseError(
          new MutationRunFailedException(
            s"Expected an object of type 'Response' from socket connection, but received $other"
          )
        )
    }
  }
}

object TestRunnerConnection {
  def create(socket: Socket)(implicit cs: ContextShift[IO]): Resource[IO, TestRunnerConnection] =
    for {
      blocker <- Blocker[IO]
      out <- Resource.fromAutoCloseable(IO(new ObjectOutputStream(socket.getOutputStream())))
      in <- Resource.fromAutoCloseable(IO(new ObjectInputStream(socket.getInputStream())))
    } yield new SocketTestRunnerConnection(blocker, out, in)

}
