package stryker4jvm.model

import fs2.io.file.Path
import stryker4jvm.core.model.{AST, MutantWithId}

final case class MutatedFile(fileOrigin: Path, mutatedSource: AST, mutants: Vector[MutantWithId[AST]])
