package io.stryker4s

import cats.effect.IO
import fs2.{Stream, StreamApp}
import io.stryker4s.endpoint.{DrinksEndpoint, HelloWorldEndpoint, OrderEndpoint}
import io.stryker4s.repository.DrinksRepository
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze._

import scala.language.higherKinds

object RoboBarServer extends StreamApp[IO] with Http4sDsl[IO] {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] val root = "/stryker4s"

  def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] = {
    Stream.eval(DrinksRepository.empty[IO]).flatMap { drinksRepository =>
      BlazeBuilder[IO]
        .bindHttp(8080, "0.0.0.0")
        .mountService(HelloWorldEndpoint.service, root)
        .mountService(OrderEndpoint.service("RoboBar"), root)
        .mountService(DrinksEndpoint.service(drinksRepository), root)
        .serve
    }
  }
}
