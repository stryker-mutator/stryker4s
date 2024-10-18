package stryker4s.testutil.stubs

import cats.Applicative
import cats.effect.std.Env
import cats.syntax.all.*

import scala.collection.immutable.Iterable

object EnvStub {
  def makeEnv[F[_]: Applicative](entr: (String, String)*): Env[F] = new Env[F] {
    val env = entr.toMap
    val downcast: Iterable[(String, String)] = env

    override def entries: F[Iterable[(String, String)]] = downcast.pure[F]
    override def get(name: String): F[Option[String]] = env.get(name).pure[F]
  }
}
