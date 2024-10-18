package stryker4s.run.process

import cats.effect.{IO, Resource}

import scala.sys.process.{ProcessBuilder, ProcessLogger}

object ProcessResource {
  def fromProcessBuilder(pb: => ProcessBuilder)(logger: String => Unit) = {
    val processLogger = ProcessLogger(logger(_))
    Resource.make(IO(pb.run(processLogger)))(p => IO(p.destroy()))
  }
}
