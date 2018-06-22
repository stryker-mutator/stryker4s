package stryker4s.mutants.findmutants

import better.files._
import stryker4s.config.Config

trait SourceCollector {
  def collectFiles(): Iterable[File]
}

class FileCollector(implicit config: Config) extends SourceCollector {

  override def collectFiles(): Iterable[File] =
    config.baseDir
      .glob(config.files.mkString("{", ",", "}"))
      .toIterable

}
