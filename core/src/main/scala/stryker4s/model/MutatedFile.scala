package stryker4s.model

import fs2.io.file.Path
import stryker4s.mutants.tree.MutationsWithId

final case class MutatedFile(
    fileOrigin: Path,
    mutatedSource: String,
    mutants: MutationsWithId
    // nonCompilingMutants: Seq[Mutant],
    // excludedMutants: Int
)
