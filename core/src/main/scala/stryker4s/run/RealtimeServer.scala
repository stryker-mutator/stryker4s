package stryker4s.run

import cats.effect.{Async, ExitCode, IO, Resource}
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Server
import org.http4s.server.middleware.Logger

object RealtimeServer {
  private val serverResource: Resource[IO, Server] = createServerResource

  def run: IO[ExitCode] = {
    IO(println("Starting server")) *> serverResource.use(_ => IO.never).as(ExitCode.Success)
  }

  def stop: IO[Unit] = {
    serverResource.use(_ => IO.unit) *> IO(println("Server stopped"))
  }

  private def createServerResource: Resource[IO, Server] = {

    val httpApp = RealtimeRoutes.report().orNotFound

    val finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(finalHttpApp)
      .build
  }
}
