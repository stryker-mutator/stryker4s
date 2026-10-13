package stryker4s

import fs2.io.file.Path
import mutationtesting.MutantResult

package object model {
  type MutantResultsPerFile = Map[Path, Vector[MutantResult]]
}
