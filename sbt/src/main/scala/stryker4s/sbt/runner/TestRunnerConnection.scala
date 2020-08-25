package stryker4s.sbt.runner

import stryker4s.api.testprocess.Request
import cats.effect.IO
import cats.effect.Blocker
import java.io.ObjectOutputStream
import java.io.ObjectInputStream
import cats.effect.ContextShift
import grizzled.slf4j.Logging
import stryker4s.api.testprocess.Response
import scala.tools.nsc.io.Socket
import cats.effect.Resource
import stryker4s.extension.exception.MutationRunFailedException

sealed trait TestRunnerConnection {
  def sendMessage(request: Request): IO[Response]
}

final class SocketTestRunnerConnection(blocker: Blocker, out: ObjectOutputStream, in: ObjectInputStream)(implicit
    cs: ContextShift[IO]
) extends TestRunnerConnection
    with Logging {

  override def sendMessage(request: Request): IO[Response] = {
    blocker.delay[IO, Unit](debug(s"Sending message $request")) *>
      blocker.delay[IO, Unit](out.writeObject(request)) *>
      // Block until a response is read.
      blocker.delay[IO, Any](in.readObject()) flatMap {
      case response: Response =>
        blocker.delay[IO, Response] {
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
      out <- Resource.fromAutoCloseable(IO(new ObjectOutputStream(socket.outputStream())))
      in <- Resource.fromAutoCloseable(IO(new ObjectInputStream(socket.inputStream())))
    } yield new SocketTestRunnerConnection(blocker, out, in)

}
