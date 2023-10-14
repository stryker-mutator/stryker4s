package stryker4s.run

import cats.effect.{IO, Sync}
import cats.syntax.all.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object RealtimeRoutes extends Http4sDsl[IO] {
  def report(): HttpRoutes[IO] = {
    HttpRoutes.of[IO] { case GET -> Root / "sse" =>
      // Turn the stream into an SSE response
      Ok()
    }

  }
}
