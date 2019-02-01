package io.stryker4s.endpoint

import cats.data.Kleisli
import cats.effect.IO
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.{Request, Response, Status, Uri}
import org.scalatest.{FunSpec, Matchers}

class HealthCheckTest extends FunSpec with Matchers with Http4sDsl[IO] with Http4sClientDsl[IO] {

  private[this] val sut: Kleisli[IO, Request[IO], Response[IO]] = HealthCheck.endpoints[IO]().orNotFound

  describe("A health check api") {

    it("returns 200 when status is OK") {
      (for {
        request <- GET(Uri.uri("/health"))
        response <- sut.run(request)
      } yield {
        response.status shouldBe Status.Ok
      }).unsafeRunSync
    }

    it("should return UP") {
      (for {
        request <- GET(Uri.uri("/health"))
        response <- sut.run(request)
        responseString <- response.as[String]
      } yield {
        response.status shouldEqual Ok
        responseString shouldBe """{"health":"UP"}"""
      }).unsafeRunSync
    }
  }
}