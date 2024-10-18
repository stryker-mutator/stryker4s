package stryker4s.testutil.stubs

import cats.effect.IO
import fs2.io.file.Path
import stryker4s.config.Config
import stryker4s.log.Logger
import stryker4s.mutants.findmutants.MutantFinder

import scala.meta.Source

class MutantFinderStub(source: Source)(implicit config: Config, log: Logger) extends MutantFinder {
  override def parseFile(file: Path): IO[Source] = IO.pure(source)
}
