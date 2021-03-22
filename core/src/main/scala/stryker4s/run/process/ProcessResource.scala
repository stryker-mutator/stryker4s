package stryker4s.run.process

import scala.sys.process.{ProcessBuilder, ProcessLogger}

import cats.effect.{IO, Resource}

object ProcessResource {
  def fromProcessBuilder(pb: => ProcessBuilder)(logger: String => Unit) = for {
    startedProcess <- Resource.eval(IO(pb))
    process <- Resource.make(IO(startedProcess.run(ProcessLogger(logger(_)))))(p => IO(p.destroy()))
  } yield process
}
