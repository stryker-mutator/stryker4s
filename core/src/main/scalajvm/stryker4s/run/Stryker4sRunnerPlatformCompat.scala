package stryker4s.run

import cats.effect.{IO, Resource}
import sttp.client3.SttpBackend
import sttp.client3.httpclient.fs2.HttpClientFs2Backend

trait Stryker4sRunnerPlatformCompat {
  def httpBackend: Resource[IO, SttpBackend[IO, Any]] =
    // Catch if the user runs the dashboard on Java <11
    try HttpClientFs2Backend.resource[IO]()
    catch {
      case e: BootstrapMethodError =>
        // Wrap in a UnsupportedOperationException because BootstrapMethodError will not be caught
        Resource.raiseError[IO, Nothing, Throwable](
          new UnsupportedOperationException(
            "Could not send results to dashboard. The dashboard reporter only supports JDK 11 or above. If you are running on a lower Java version please upgrade or disable the dashboard reporter.",
            e
          )
        )
    }
}
