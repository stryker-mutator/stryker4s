package io.stryker4s.endpoint

import cats.effect.IO
import io.circe.Json
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._

import scala.language.higherKinds

object HelloWorldEndpoint {

  val service: HttpService[IO] = {
    HttpService[IO] {
      case GET -> Root / "hello" / name =>
        Ok(Json.obj("message" -> Json.fromString(s"Hello, $name")))
    }
  }
}
