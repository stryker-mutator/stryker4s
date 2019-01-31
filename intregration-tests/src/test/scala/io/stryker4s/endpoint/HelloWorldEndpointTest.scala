package io.stryker4s.endpoint

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import org.scalatest.{FunSpec, Matchers}

class HelloWorldEndpointTest extends FunSpec with Matchers {

  describe("A hello world api") {

    it("returns 200 when status is OK") {
      val response = retHelloWorld

      response.status shouldBe Status.Ok
    }

    it("should return hello world") {
      val response = retHelloWorld

      response.as[String].unsafeRunSync() shouldBe """{"message":"Hello, world"}"""
    }
  }

  private[this] val retHelloWorld: Response[IO] = {
    val getHW = Request[IO](Method.GET, Uri.uri("/hello/world"))
    HelloWorldEndpoint.service.orNotFound(getHW).unsafeRunSync()
  }
}