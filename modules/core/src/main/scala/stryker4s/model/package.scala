package stryker4s

import fs2.io.file.Path
import mutationtesting.MutantResult

package object model {
  type MutantResultsPerFile = Map[Path, Vector[MutantResult]]

  // The types below now live in the public `stryker4s-mutator-api` module (`stryker4s.mutatorapi`), so that
  // third-party custom mutators can depend on them without pulling in all of `core`. They're re-exported here
  // (rather than moving every usage across `core`) to keep this a source-compatible relocation.
  type PlaceableTree = stryker4s.mutatorapi.PlaceableTree
  val PlaceableTree: stryker4s.mutatorapi.PlaceableTree.type = stryker4s.mutatorapi.PlaceableTree

  type IgnoredMutationReason = stryker4s.mutatorapi.IgnoredMutationReason
  val MutationExcluded: stryker4s.mutatorapi.MutationExcluded.type = stryker4s.mutatorapi.MutationExcluded
  type RegexParseError = stryker4s.mutatorapi.RegexParseError
  val RegexParseError: stryker4s.mutatorapi.RegexParseError.type = stryker4s.mutatorapi.RegexParseError
  type NoRegexMutationsFound = stryker4s.mutatorapi.NoRegexMutationsFound
  val NoRegexMutationsFound: stryker4s.mutatorapi.NoRegexMutationsFound.type =
    stryker4s.mutatorapi.NoRegexMutationsFound

  type MutantMetadata = stryker4s.mutatorapi.MutantMetadata
  val MutantMetadata: stryker4s.mutatorapi.MutantMetadata.type = stryker4s.mutatorapi.MutantMetadata

  type MutatedCode = stryker4s.mutatorapi.MutatedCode
  val MutatedCode: stryker4s.mutatorapi.MutatedCode.type = stryker4s.mutatorapi.MutatedCode
}
