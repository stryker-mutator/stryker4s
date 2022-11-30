package stryker4jvm.model

import fs2.io.file.Path
import stryker4jvm.core.model.AST

final case class SourceContext(source: AST, path: Path)
