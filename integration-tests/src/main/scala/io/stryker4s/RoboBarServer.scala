package io.stryker4s

import cats.effect._
import cats.implicits._
import io.stryker4s.endpoint.{DrinksEndpoint, HealthCheck, OrderEndpoint}
import io.stryker4s.repository.DrinksRepository
import org.http4s.implicits._
import org.http4s.server.blaze._
import org.http4s.server.{Router, Server}

import scala.language.higherKinds

object RoboBarServer extends IOApp {

  private[this] val root = "/stryker4s"

  def run(args: List[String]): IO[ExitCode] = createServer().use(_ => IO.never).as(ExitCode.Success)

  def createServer[F[_] : ContextShift : ConcurrentEffect : Timer](): Resource[F, Server[F]] = {
    val drinksRepository = DrinksRepository[F]()
    val services = HealthCheck.endpoints() <+>
      OrderEndpoint.endpoints("RoboBar") <+>
      DrinksEndpoint.endpoints(drinksRepository)
    val httpApp = Router(root -> services).orNotFound

    BlazeServerBuilder[F]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(httpApp)
      .resource

  }
}
