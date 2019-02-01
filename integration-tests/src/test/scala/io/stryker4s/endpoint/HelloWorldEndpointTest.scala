package io.stryker4s.endpoint

import cats.data.Kleisli
import cats.effect.IO
import org.http4s.{Request, Response, Status, Uri}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.scalatest.{FunSpec, Matchers}

class HelloWorldEndpointTest extends FunSpec with Matchers with Http4sDsl[IO] with Http4sClientDsl[IO] {

  private[this] val sut: Kleisli[IO, Request[IO], Response[IO]] = HelloWorldEndpoint.endpoints[IO]().orNotFound

  describe("A hello world api") {

    it("returns 200 when status is OK") {
      for {
        request <- GET(Uri.uri("/hello/world"))
        response <- sut.run(request)
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    it("should return hello world") {
      for {
        request <- GET(Uri.uri("/hello/world"))
        response <- sut.run(request)
      } yield {
        response.as[String].unsafeRunSync() shouldBe """{"message":"Hello, world"}"""
      }
    }
  }
}