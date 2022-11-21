package stryker4jvm.model

import fs2.io.file.Path
import stryker4jvm.core.model.AST
import stryker4jvm.mutants.tree.MutantsWithId

final case class MutatedFile(fileOrigin: Path, mutatedSource: AST, mutants: MutantsWithId)
