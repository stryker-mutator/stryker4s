package stryker4jvm.testutil.stubs

import cats.effect.IO
import fs2.io.file.Path
import stryker4jvm.config.Config
import stryker4jvm.core.logging.Logger
import stryker4jvm.mutants.findmutants.MutantFinder

import scala.meta.Source

class MutantFinderStub(source: Source)(implicit config: Config, log: Logger) extends MutantFinder {
  override def parseFile(file: Path): IO[Source] = IO.pure(source)
}
