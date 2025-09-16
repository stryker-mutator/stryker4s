package stryker4s.run.process

import cats.effect.IO
import cats.syntax.flatMap.*
import fs2.Pipe
import fs2.io.process.ProcessBuilder
import fs2.text.{lines, utf8}

object ProcessResource {
  def fromProcessBuilder(pb: ProcessBuilder, logger: Option[String => IO[Unit]]) = {
    def log(logger: String => IO[Unit]): Pipe[IO, Byte, Unit] = _.through(utf8.decode).through(lines).evalMap(logger)

    val process = pb.spawn[IO]

    logger.fold(process) { logger =>
      val logPipe = log(logger)
      process.flatTap(p => p.stdout.through(logPipe).concurrently(p.stderr.through(logPipe)).compile.drain.background)
    }
  }
}
