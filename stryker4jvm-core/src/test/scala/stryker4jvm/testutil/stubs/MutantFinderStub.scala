package stryker4jvm.testutil.stubs

import cats.effect.IO
import fs2.io.file.Path

import scala.meta.Source

class MutantFinderStub(source: Source)(implicit config: Config, log: Logger) extends MutantFinder {
  override def parseFile(file: Path): IO[Source] = IO.pure(source)
}
