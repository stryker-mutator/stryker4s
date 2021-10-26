package stryker4s.run

import cats.effect.{IO, Resource}
import sttp.client3.SttpBackend
import sttp.client3.impl.cats.FetchCatsBackend

trait Stryker4sRunnerPlatformCompat {
  def httpBackend: Resource[IO, SttpBackend[IO, Any]] = Resource.pure(FetchCatsBackend[IO]())
}
