package stryker4s.run.process

import cats.effect.IO
import cats.syntax.flatMap.*
import fs2.Stream
import fs2.io.process.ProcessBuilder
import fs2.text.{lines, utf8}

object ProcessResource {
  def fromProcessBuilder(pb: ProcessBuilder, logger: String => IO[Unit]) = {
    def log(s: Stream[IO, Byte]) = s.through(utf8.decode).through(lines).evalMap(logger)

    pb.spawn[IO].flatTap(p => log(p.stdout).concurrently(log(p.stderr)).compile.drain.background)
  }
}
