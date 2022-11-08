package stryker4jvm.model

import fs2.io.file.Path

import scala.meta.Source

final case class SourceContext(source: Source, path: Path)
