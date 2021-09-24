package stryker4s.run.process

import scala.sys.process.{ProcessBuilder, ProcessLogger}

import cats.effect.{IO, Resource}

object ProcessResource {
  def fromProcessBuilder(pb: => ProcessBuilder)(logger: String => Unit) = {
    val processLogger = ProcessLogger(logger(_))
    Resource.make(IO(pb.run(processLogger)))(p => IO(p.destroy()))
  }
}
